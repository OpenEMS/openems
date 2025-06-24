#!/usr/bin/env ruby

require 'xcodeproj'
require 'plist'
require 'fileutils'

def colorize(text, color_code)
  "\e[#{color_code}m#{text}\e[0m"
end

def red(text); colorize(text, 31); end
def green(text); colorize(text, 32); end

# Extracts the MARKETING_VERSION from a target/scheme and configuration
def extract_marketing_version(target_name, configuration, project_path)
  project = Xcodeproj::Project.open(project_path)
  # Find the target
  target = project.targets.find { |t| t.name == target_name }
  
  if target
    # Get the build settings for the specified configuration
    build_settings = target.build_configurations.find { |config| config.name == configuration }.build_settings
  
    # Extract the marketing version (MARKETING_VERSION)
    marketing_version = build_settings['MARKETING_VERSION']
    puts "Marketing Version for target '#{target_name}' and configuration '#{configuration}': #{marketing_version}"
    return marketing_version
  else
    puts red("Target '#{target_name}' not found.")
  end
end

# Updates the marketing version of an app
# 
# located in App.xcodeproj
def update_marketing_version(target_name, configuration, project_path, new_marketing_version)
  project = Xcodeproj::Project.open(project_path)

  # Find the target
  target = project.targets.find { |t| t.name == target_name }
  
  if target
    # Find the build configuration for the specified configuration name
    build_configuration = target.build_configurations.find { |config| config.name == configuration }
  
    if build_configuration
      # Update the MARKETING_VERSION in the build settings
      build_configuration.build_settings['MARKETING_VERSION'] = new_marketing_version
  
      # Save the changes to the project file
      project.save
  
      puts green("Updated MARKETING_VERSION to '#{new_marketing_version}' for target '#{target_name}' and configuration '#{configuration}'.")
    else
      puts red("Configuration '#{configuration}' not found for target '#{target_name}'.")
    end
  else
    puts red("Target '#{target_name}' not found.")
  end
end

# Replaces one file with another
def replace_file(target_file, source_file)
  # Check if files exist
  unless File.exist?(source_file)
    puts "Error: Source file '#{source_file}' does not exist."
    return
  end

  unless File.exist?(target_file)
    puts "Error: Target file '#{target_file}' does not exist."
    return
  end

  # Create a backup of the target file
  backup_file = "#{target_file}.bak"
  FileUtils.cp(target_file, backup_file)
  puts "Created backup of #{target_file} as #{backup_file}"

  # Read the contents of the source file
  source_content = File.read(source_file)

  # Write the contents to the target file (overwriting it)
  File.open(target_file, 'w') do |file|
    file.write(source_content)
  end

  puts "Replaced #{target_file} with #{source_file}"
end

# Gets the package id for a scheme
def get_package_id(target_name)

  case target_name
  when "EXAMPLE"
    "io.openems.ios"
  else
    raise "Implement package_id"
  end
end

# Check if the correct number of arguments is provided
if ARGV.length != 1
  puts "Usage: ruby update_marketing_version.rb <target_name>"
  exit 1
end

target_name = ARGV[0]

# Constants
target_configuration = "Release"
project_path = 'App.xcodeproj'
local_pbxproj_path= "#{project_path}/project.pbxproj"
global_pbxproj_path= "/opt/mac-build-env/project.pbxproj"


replace_file(local_pbxproj_path, global_pbxproj_path)                                               # replace project.pbxproj
previous_version = extract_marketing_version(target_name, target_configuration, project_path).to_i  # extract MARKETING_VERSION from pbxproj
new_version = previous_version + 1                                                                  # bump version by 1
update_marketing_version(target_name, target_configuration, project_path, new_version)              # update marketing version
replace_file(global_pbxproj_path, local_pbxproj_path)                                               # replace global file