# Add .gitignore file to empty test directories.
# 
# When Eclipse creates 'test' src folders they are sometimes empty. Empty 
# folders are not committed to GIT. Because of this Eclipse would show errors
# when importing the projects. This script creates an empty '.gitignore' file
# inside each 'test' folder to solve this.
#
# See https://stackoverflow.com/questions/115983
#
for D in *; do
	if [ -d "${D}" ]; then
		case "${D}" in
			build|cnf|doc|edge|ui|tools)
				;;
			*)
				# verify the project .gitignore file
				if grep -q '/bin_test/' ${D}/.gitignore \
					&& grep -q '/generated/' ${D}/.gitignore; then
					continue
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
				;;
		esac
	fi
done
