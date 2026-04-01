#!/bin/bash

################################################################################
# OpenEMS Bundle README Standardization Script
# 
# Purpose: Guide and automate the standardization of bundle readme.adoc files
#          according to the OpenEMS standard structure
#
# Usage:   ./standardize_bundle_readme.sh <bundle_name>
#
# Example: ./standardize_bundle_readme.sh io.openems.edge.meter.weidmueller
#
# Prerequisites:
#   - Bash 4.0+
#   - ripgrep (rg) for fast code search
#   - Bundle source code accessible
#
# Output:
#   - Extracted component metadata printed to stdout
#   - Copy-paste ready template provided
#   - Build verification instructions
#
################################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REPO_ROOT="/home/stefan/git/openems"
BUNDLE_PATTERN="${1:-.}"

# Functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_section() {
    echo -e "\n${YELLOW}>>> $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_section "Checking prerequisites"
    
    if ! command -v rg &> /dev/null; then
        print_warning "ripgrep not found. Using grep instead (slower)"
        GREP_CMD="grep"
    else
        GREP_CMD="rg"
        print_success "ripgrep found"
    fi
    
    if [ ! -d "$REPO_ROOT" ]; then
        print_error "Repository not found at $REPO_ROOT"
        exit 1
    fi
    print_success "Repository found at $REPO_ROOT"
}

# Find bundle directory
find_bundle() {
    local pattern="$1"
    
    print_section "Searching for bundle: $pattern"
    
    local bundle_dir=$(find "$REPO_ROOT" -maxdepth 1 -type d -name "*$pattern*" 2>/dev/null | head -1)
    
    if [ -z "$bundle_dir" ]; then
        print_error "Bundle not found matching: $pattern"
        echo -e "\n${YELLOW}Available bundles:${NC}"
        ls -d "$REPO_ROOT"/io.openems.* | head -20
        exit 1
    fi
    
    print_success "Found bundle: $bundle_dir"
    echo "$bundle_dir"
}

# Extract implementation files
find_impl_files() {
    local bundle_dir="$1"
    
    print_section "Finding implementation files"
    
    local impl_files=$(find "$bundle_dir" -path "*/src/*" -name "*Impl.java" 2>/dev/null | sort)
    
    if [ -z "$impl_files" ]; then
        print_warning "No *Impl.java files found - this may be an API-only bundle"
        return 1
    fi
    
    echo "$impl_files"
}

# Extract component metadata
extract_component_info() {
    local impl_file="$1"
    local bundle_dir="$2"
    
    print_section "Extracting component info from: $(basename "$impl_file")"
    
    # Find corresponding Config.java
    local config_file="${impl_file%/*}/Config.java"
    if [ ! -f "$config_file" ]; then
        config_file=$(dirname "$impl_file")/Config.java
        if [ ! -f "$config_file" ]; then
            print_warning "Config.java not found for $impl_file"
            return 1
        fi
    fi
    
    print_info "Using config: $(basename "$config_file")"
    
    # Extract Factory-PID
    local factory_pid=$(grep -A 5 "@Component" "$impl_file" 2>/dev/null | grep 'name = "' | head -1 | grep -o '"[^"]*"' | tr -d '"' || echo "UNKNOWN")
    print_success "Factory-PID: $factory_pid"
    
    # Extract Component class name
    local class_name=$(grep "^public class" "$impl_file" | awk '{print $3}' | cut -d' ' -f1 || echo "UNKNOWN")
    print_success "Class: $class_name"
    
    # Extract implements
    local implements=$(grep "public class" "$impl_file" | grep "implements" | sed 's/.*implements //' | tr -d '{' || echo "UNKNOWN")
    print_success "Implements: $implements"
    
    # Extract ObjectClassDefinition name
    local obj_name=$(grep -A 2 "@ObjectClassDefinition" "$config_file" | grep 'name = "' | head -1 | grep -o '"[^"]*"' | tr -d '"' || echo "UNKNOWN")
    print_success "Display Name: $obj_name"
    
    # Extract ObjectClassDefinition description
    local description=$(grep -A 3 "@ObjectClassDefinition" "$config_file" | grep 'description' | head -1 | sed 's/.*description[[:space:]]*=[[:space:]]*"//' | sed 's/".*//' || echo "")
    if [ -n "$description" ]; then
        print_success "Description: $description"
    fi
    
    # Extract configuration parameters
    print_info "Configuration Parameters:"
    echo "---"
    grep "@AttributeDefinition" "$config_file" -A 1 | grep -v "^--$" | paste - - | sed 's/@AttributeDefinition(name = "//' | sed 's/String[[:space:]]*\(.*\)().*default: "\([^"]*\)"/\1 (String) [default: \2]/' | sed 's/String[[:space:]]*\(.*\)();.*/\1 (String)/' | sed 's/boolean[[:space:]]*\(.*\)().*default: \(true\|false\)/\1 (Boolean) [default: \2]/' | sed 's/int[[:space:]]*\(.*\)().*default: \([0-9]*\)/\1 (Integer) [default: \2]/' | sed 's/MeterType[[:space:]]*\(.*\)().*default: \(.*\);/\1 (MeterType) [default: \2]/' | head -15
    echo "---"
    
    # Output summary for template
    cat << EOF

${GREEN}TEMPLATE PLACEHOLDERS:${NC}

Factory-PID:        $factory_pid
Display Name:       $obj_name
Class:              $class_name
Implements:         $implements

EOF
}

# Generate template
generate_template() {
    local bundle_name="$1"
    local bundle_path="$2"
    
    # Extract bundle readable name
    local bundle_readable=$(echo "$bundle_path" | sed 's|.*/||' | sed 's/^io\.openems\.edge\.//' | sed 's/\./\n/g' | head -1 | tr '[:lower:]' '[:upper:]' | sed 's/^\(.\)/\1/')
    
    print_section "README Template (copy to $bundle_path/readme.adoc)"
    
    cat << 'TEMPLATE'

= Bundle Title

One-line description.

== Overview

2-3 sentences describing what the bundle provides, its purpose, and typical use cases.

== Supported Devices

.Device Name
* **Type**: Device type
* **Communication**: Protocol (e.g., Modbus TCP)
* **Typical Use**: Primary use cases
* **Documentation**: https://example.com[Link icon:external-link[]]

== Components

This bundle implements the following OpenEMS Components:

=== <<_component_id,Display Name>>

*Name*: Display Name from @ObjectClassDefinition

*Factory-PID*: `Factory.PID.From.Component`

.Implemented Natures/Interfaces
* Nature1
* Nature2

*Description*: What this component does and why you'd use it.

.*Configuration*:
* `id` (String): Component ID (default: "meter0")
* `alias` (String): Human-readable alias
* `enabled` (Boolean): Enable/disable (default: true)
* `custom_param` (Type): Description (default: value)

https://github.com/OpenEMS/openems/tree/develop/BUNDLE_NAME[Source Code icon:github[]]

TEMPLATE
}

# Verify build
verify_build() {
    local bundle_dir="$1"
    local bundle_name=$(basename "$bundle_dir")
    
    print_section "Building Antora documentation to verify"
    
    if cd "$REPO_ROOT" && timeout 120 ./gradlew buildAntoraDocs --quiet 2>&1 | tail -5; then
        print_success "Build completed successfully"
        return 0
    else
        print_warning "Build may have issues - check output above"
        return 1
    fi
}

# Main workflow
main() {
    print_header "OpenEMS Bundle README Standardization"
    
    if [ -z "${1:-}" ]; then
        print_error "Usage: $0 <bundle_pattern>"
        echo "Example: $0 meter.weidmueller"
        echo "Example: $0 io.openems.edge.meter.weidmueller"
        exit 1
    fi
    
    check_prerequisites
    
    local bundle_dir=$(find_bundle "$1")
    local bundle_name=$(basename "$bundle_dir")
    
    # Check if readme exists
    if [ -f "$bundle_dir/readme.adoc" ]; then
        print_info "Found existing readme.adoc ($(wc -l < "$bundle_dir/readme.adoc") lines)"
    else
        print_warning "No readme.adoc found - this is a new file"
    fi
    
    # Find implementation files
    if impl_files=$(find_impl_files "$bundle_dir"); then
        local count=$(echo "$impl_files" | wc -l)
        print_success "Found $count implementation file(s)"
        
        # Extract info for each component
        while IFS= read -r impl_file; do
            extract_component_info "$impl_file" "$bundle_dir"
        done <<< "$impl_files"
    else
        print_info "API-only or library bundle - skip implementation extraction"
    fi
    
    # Show template
    generate_template "$bundle_name" "$bundle_dir"
    
    # Next steps
    print_section "Next Steps"
    cat << EOF

1. Edit the readme.adoc template above and save to:
   $bundle_dir/readme.adoc

2. Follow these guidelines:
   - Use .Title for list titles (not *Title*:)
   - Use * for bullets (not -)
   - Add icon:external-link[] to external links
   - Add icon:github[] to GitHub links
   - Anchors use format: [[_component_id]]
   
3. Build to verify:
   cd $REPO_ROOT
   ./gradlew buildAntoraDocs --quiet

4. Commit your changes:
   git add $bundle_dir/readme.adoc
   git commit -m "Standardize $bundle_name readme.adoc"

Reference: doc/tools/OPENEMS_README_STANDARDIZATION.md

EOF
    
    print_success "Extraction complete!"
}

# Run main
main "$@"
