#!/usr/bin/env ruby
require_relative "shared.rb"
require "net/http"
require "json"
require "uri"
require "optparse"


options = { bundleId: nil, version: nil, authenticationKeyFile: nil }

OptionParser.new do |opts|
  opts.banner = "Usage: create_release.rb --bundle-id VALUE --version VALUE --authFile VALUE"
  
  opts.on("--bundle-id VALUE", "First required argument") do |v|
    options[:bundleId] = v
  end
  opts.on("--version VALUE", "Second required argument") do |v|
    options[:version] = v
  end
  opts.on("--authFile VALUE", "Third required argument") do |v|
    options[:authenticationKeyFile] = v
  end
end.parse!

BUNDLE_ID = options[:bundleId]
VERSION = options[:version]
TEMP_JWT = generate_jwt(options[:authenticationKeyFile])
API = "https://api.appstoreconnect.apple.com/v1"

def request(method, path, body = nil)
  uri = URI("#{API}#{path}")

  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true

  klass = case method
          when :get then Net::HTTP::Get
          when :post then Net::HTTP::Post
          when :patch then Net::HTTP::Patch
          else
            raise "Unsupported method"
          end

  req = klass.new(uri)
  req["Authorization"] = "Bearer #{TEMP_JWT}"
  req["Content-Type"] = "application/json"

  req.body = JSON.dump(body) if body

  res = http.request(req)

  unless res.is_a?(Net::HTTPSuccess)
    abort <<~MSG
      Request failed: #{res.code}
      #{res.body}
    MSG
  end

  if res.body
    JSON.parse(res.body)
  end
end

# Gets the app id from a given bundle id
def get_app_id()
  uri = URI("https://api.appstoreconnect.apple.com/v1/apps?filter[bundleId]=#{BUNDLE_ID}")
  request = Net::HTTP::Get.new(uri)
  request["Authorization"] = "Bearer #{TEMP_JWT}"
  request["Content-Type"] = "application/json"
  
  response = Net::HTTP.start(uri.hostname, uri.port, use_ssl: true) do |http|
    http.request(request)
  end
  
  if response.is_a?(Net::HTTPSuccess)
    data = JSON.parse(response.body)
    
    app_id = data.dig("data", 0, "id")
    
    return app_id
  else
    puts "API error: #{response.code}"
    puts response.body
  end
end

# Finds the lastest uploaded build for a given app id
def find_latest_build(app_id)
  response = request(
    :get,
    "/builds?filter[app]=#{app_id}&sort=-uploadedDate&limit=1"
  )
    
  response.fetch("data", []).first
end
  
# Finds the latest version for a given app id
def find_version(app_id)
  response = request(
    :get,
    "/apps/#{app_id}/appStoreVersions"
  )
    
  response.fetch("data", []).first
end

# Creates a review submission for a given app id
def create_review_submission(app_id)
  response = request(
    :post,
    "/reviewSubmissions",
    {
      data: {
        type: "reviewSubmissions",
        relationships: {
          app: {
            data: {
              type: "apps",
              id: app_id
            }
          }
        }
      }
    }
  )
  response.fetch("data")
end

# Adds a version to the current draft submission
def add_version_to_submission(submission_id, version_id)
  request(
    :post,
    "/reviewSubmissionItems",
    {
      data: {
        type: "reviewSubmissionItems",
        relationships: {
          reviewSubmission: {
            data: {
              type: "reviewSubmissions",
              id: submission_id
            }
          },
          appStoreVersion: {
            data: {
              type: "appStoreVersions",
              id: version_id
            }
          }
        }
      }
    }
  )
end


def find_version_localization(version_id)
  request(
    :get,
    "/appStoreVersions/#{version_id}/appStoreVersionLocalizations",
  )
end
  
def update_whats_new(localization_id, text)
  request(
    :patch,
    "/appStoreVersionLocalizations/#{localization_id}",
    {
      data: {
        type: "appStoreVersionLocalizations",
        id: localization_id,
        attributes: {
          whatsNew: text
        }
      }
    }
    )
end
    
def submit_for_review(submission_id)
  request(
    :patch,
    "/reviewSubmissions/#{submission_id}",
    {
      data: {
        type: "reviewSubmissions",
        id: submission_id,
        attributes: {
          submitted: true
        }
      }
    }
  )
end

APP_ID = get_app_id()

version = find_version(APP_ID)
build = find_latest_build(APP_ID)

if version
  version_id = version["id"]
  version_string = version["attributes"]["versionString"]
  state = version.dig("attributes", "appStoreState")

  puts "Found existing version #{version_string} (#{state})"

  unless %w[PREPARE_FOR_SUBMISSION DEVELOPER_REJECTED].include?(state)
    abort "Version is not editable (#{state})"
  end

  request(
    :patch,
    "/appStoreVersions/#{version_id}/relationships/build",
    {
      data: {
        type: "builds",
        id: build["id"]
      }
    }
  )

  puts "Assigned build #{build["attributes"]["version"]} to version #{version_string}"
else
    puts "No existing version found. Creating a new one."
end

localizations = find_version_localization(version["id"])
localization = localizations["data"].first

update_whats_new(
  localization["id"],
  "Bug fixes and performance improvements."
)

submission = create_review_submission(APP_ID)

add_version_to_submission(
 submission["id"],
  version["id"]
)

submit_for_review(submission["id"])
puts "Submitted for review"