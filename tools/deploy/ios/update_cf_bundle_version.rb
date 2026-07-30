require_relative 'shared.rb'
require 'plist'
require 'optparse'



options = { scheme: nil, targetName: nil }

OptionParser.new do |opts|
  opts.banner = "Usage: update_cf_bundle_version.rb --scheme VALUE --target-name VALUE"
  
  opts.on("--scheme VALUE", "First required argument") do |v|
    options[:scheme] = v
  end
  
  opts.on("--target-name VALUE", "Second required argument") do |v|
    options[:targetName] = v
  end
end.parse!

if options[:scheme].nil? || options[:targetName].nil?
  puts "Error: Both --scheme and --target-name are required"
  puts OptionParser.new.help
  exit 1
end

puts "Arguments: #{options[:scheme]}, #{options[:targetName]}"

target_configuration = "Release"
project_path = "#{options[:scheme]}.xcodeproj"

marketing_version = extract_marketing_version(options[:targetName], target_configuration, project_path).to_i  # extract MARKETING_VERSION from pbxproj

puts "Version: #{marketing_version}"

info_plist_path = "output/#{options[:scheme]}.xcarchive/Info.plist";
plist = Plist.parse_xml(info_plist_path);
    File.delete(info_plist_path)
plist['ApplicationProperties']['CFBundleShortVersionString'] = "#{marketing_version}"
plist.save_plist(info_plist_path)