#!/bin/bash

# GitHub repository details
REPO="OpenEMS/openems"
FILE_NAME="openems-backend.jar"

# Get the latest release URL
release_url=$(curl -s "https://api.github.com/repos/${REPO}/releases/latest" | jq -r '.assets[0].browser_download_url')

if [ -z "$release_url" ]; then
  echo "Failed to retrieve the release URL. Please check the repository or try again later."
  exit 1
fi

# Download the binary file
echo "Downloading ${FILE_NAME} from the latest release..."
curl -LJO "${release_url}"

echo "Download complete. The ${FILE_NAME} file is saved in the current directory."

mkdir -p build
mv ${FILE_NAME} build/
