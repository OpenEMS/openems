#!/bin/bash -e
#
# Prepares a Commit
#
# - Adds .gitignore file to empty test directories.
#
#   When Eclipse creates 'test' src folders they are sometimes empty. Empty
#   folders are not committed to GIT. Because of this Eclipse would show errors
#   when importing the projects. This script creates an empty '.gitignore' file
#   inside each 'test' folder to solve this.
#
#   See https://stackoverflow.com/questions/115983
#
# - Resets .classpath files.
#
#   When Eclipse 'Build All' is called, all .classpath files are touched and
#   unnecessarily marked as changed. Using this script those files are reset
#   to origin.
#
# - Resolves EdgeApp and BackendApp bndrun files
#

# Check bundles
for D in *; do
	if [ -d "${D}" ]; then
		case "${D}" in
			build|cnf|doc|edge|ui|tools)
				;;
			*)

				# check for empty/non-project directories
				if [ ! -d "${D}/src" ]; then
					echo "${D} is empty. Delete directory?"
					select yn in "Yes" "No"; do
						case $yn in
							Yes ) rm -rf "${D}"; break;;
							No ) ;;
						esac
					done
					continue
				fi

				echo "# preparing ${D}"

				# verify the project .gitignore file
				if grep -q '/bin_test/' ${D}/.gitignore \
					&& grep -q '/generated/' ${D}/.gitignore; then
					:
				else
					echo "${D}/.gitignore -> not complete"
					echo '/bin_test/' > ${D}/.gitignore
					echo '/generated/' >> ${D}/.gitignore
				fi

				# verify there is a test folder
				if [ ! -d "${D}/test" ]; then
					mkdir -p ${D}/test
				fi

				# verify that the test folder has a .gitignore file
				if [ ! -f "./${D}/test/.gitignore" ]; then
					echo "${D}/test/.gitignore -> missing"
					touch ${D}/test/.gitignore
				fi

				# verify explicit encoding for Eclipse IDE; avoids 'Project has no explicit encoding set' warnings
				if [ ! -f "./${D}/.settings/org.eclipse.core.resources.prefs" ]; then
					echo "${D}/.settings/org.eclipse.core.resources.prefs -> missing"
					mkdir "${D}/.settings"
					cat <<EOT > "${D}/.settings/org.eclipse.core.resources.prefs"
eclipse.preferences.version=1
encoding/<project>=UTF-8
EOT
				fi

				# Set default .classpath file
				cat <<EOT > "${D}/.classpath"
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="con" path="aQute.bnd.classpath.container"/>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-21"/>
	<classpathentry kind="src" output="bin" path="src"/>
	<classpathentry kind="src" output="bin_test" path="test">
		<attributes>
			<attribute name="test" value="true"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="output" path="bin"/>
</classpath>
EOT

				# Set default .project file
				cat <<EOT > "${D}/.project"
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>${D}</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>bndtools.core.bndbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
		<nature>bndtools.core.bndnature</nature>
	</natures>
</projectDescription>
EOT

				# reset tools/docker/ui/docker-compose.yml to standard in case of WSL2
				if grep -qi microsoft /proc/version 2>/dev/null; then
					# running in WSL2
				    sed -i "s/host\.docker\.internal:[0-9]\+\.[0-9]\+\.[0-9]\+\.[0-9]\+/host.docker.internal:host-gateway/" docker-compose.yml
				    echo "✓ Set docker-compose.yml back to default, using host-gateway"
				fi

				# Verify bnd.bnd file
				if [ -f "${D}/bnd.bnd" ]; then
					start=$(grep -n '${buildpath},' "${D}/bnd.bnd" | grep -Eo '^[^:]+' | head -n1)
					end=$(grep -n 'testpath' "${D}/bnd.bnd" | grep -Eo '^[^:]+' | head -n1)
					if [ -z "$start" -a -z "$end" ]; then
						:
					else
						(
							head -n $start "${D}/bnd.bnd"; # before 'buildpath'
							head -n$(expr $end - 2) "${D}/bnd.bnd" | tail -n$(expr $end - $start - 2) | LC_COLLATE=C sort | sed '/\\$/!s/$/,\\/'; # the 'buildpath'
							tail -n +$(expr $end - 1) "${D}/bnd.bnd" # after 'buildpath'
						) > "${D}/bnd.bnd.new"
						if [ $? -eq 0 ]; then
							mv "${D}/bnd.bnd.new" "${D}/bnd.bnd"
						else
							echo "Unable to sort buildpath in ${D}/bnd.bnd"
							exit 1
						fi
					fi
				fi
				;;
		esac
	fi
done

# Build
echo "#"
echo "# building Java projects"
./gradlew build

update_bndrun() {
	echo "#"
	echo "# updating $1"
	local bndrun="${2}.application/${1}.bndrun"
	head -n $(grep -n '\-runrequires:' $bndrun | grep -Eo '^[^:]+' | head -n1) "$bndrun" > "$bndrun.new"
	echo "	bnd.identity;id='org.ops4j.pax.logging.pax-logging-api',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.ops4j.pax.logging.pax-logging-log4j2',\\" >> "$bndrun.new"
	if [[ "$1" == "BackendApp" ]]; then
		echo "	bnd.identity;id='org.osgi.service.jdbc',\\" >> "$bndrun.new"
	fi
	echo "	bnd.identity;id='org.apache.felix.http.jetty12',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.apache.felix.http.servlet-api',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.apache.felix.webconsole',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.apache.felix.webconsole.plugins.ds',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.apache.felix.inventory',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.apache.felix.eventadmin',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.apache.felix.fileinstall',\\" >> "$bndrun.new"
	echo "	bnd.identity;id='org.apache.felix.metatype',\\" >> "$bndrun.new"
	for D in $2.*; do
		if [[ "$D" == *api ]]; then
			continue # ignore api bundle
		fi
		echo "	bnd.identity;id='${D}',\\" >> "$bndrun.new"
	done
	local runbundles=$(grep -n '\-runbundles:' $bndrun | grep -Eo '^[^:]+' | head -n1)
	tail -n +$(expr $runbundles - 1) "$bndrun" >> "$bndrun.new"
	head -n $(grep -n '\-runbundles:' "$bndrun.new" | grep -Eo '^[^:]+' | head -n1) "$bndrun.new" > "$bndrun"
	rm "$bndrun.new"
	./gradlew resolve.$1
}

update_bndrun EdgeApp 'io.openems.edge'
update_bndrun BackendApp 'io.openems.backend'

# Build + test UI
echo "#"
echo "# building UI project"

cd ui
npm install
node_modules/.bin/ng lint --fix
node_modules/.bin/tsc
node_modules/.bin/tsc-strict
node_modules/.bin/ng build -c "openems,openems-backend-prod,prod"
npm run test -- --no-watch --no-progress --browsers=ChromeHeadlessCI
cd ..

echo
echo "Finished"
