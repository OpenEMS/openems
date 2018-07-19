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
    if [ -d "${D}/test" ]; then
		if [ ! -f "./${D}/test/.gitignore" ]; then
			echo "${D}/test/.gitignore -> missing"
			touch ${D}/test/.gitignore
		fi
	fi
done
