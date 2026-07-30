require 'jwt'
require 'xcodeproj'
require 'fileutils'

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
      puts "Previous marketing Version for target '#{target_name}' and configuration '#{configuration}': #{marketing_version}"
      return marketing_version
    else
      puts "Target '#{target_name}' not found."
    end
end


def generate_jwt(auth_file)
  
  key_id = ENV.fetch("AUTHENTICATION_KEY_ID")
  issuer_id = ENV.fetch("AUTHENTICATION_KEY_ISSUER_ID")

  private_key = OpenSSL::PKey::EC.new(
      File.read(auth_file)
  )

  payload = {
      iss: issuer_id,
      aud: "appstoreconnect-v1",
      iat: Time.now.to_i,
      exp: Time.now.to_i + 1000
  }

  headers = {
      kid: key_id,
      typ: "JWT"
  }

  token = JWT.encode(
      payload,
      private_key,
      "ES256",
      headers
  )

  return token
end