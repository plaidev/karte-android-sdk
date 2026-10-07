github.dismiss_out_of_range_messages

$diff_files = (git.added_files + git.modified_files + git.deleted_files)
$modules = ["core", "inappmessaging", "notifications", "variables", "visualtracking", "inbox", "inappframe", "gradle-plugin", "debugger"]
$formatted_tags = git.tags.map { |tag| tag.strip }

# 
# Check Version
# 
# gitのtagから最新バージョンを返却する
def get_lastest_release_version(module_name)
    prefix = "#{module_name}-"
    $formatted_tags.select { |tag| tag =~ /^#{prefix}([0-9]+\.){1}[0-9]+(\.[0-9]+)$/ }
            .map { |tag| tag.delete(prefix) }
            .sort_by { |tag| Gem::Version.new(tag) }
            .last
end
def next_patch_version(base_version)
    versions = base_version.split('.')
    versions[2] = (versions[2].to_i + 1).to_s
    versions.join('.')
end

$modules.each { |module_name|
    if !$diff_files.any? { |file| file.start_with?("#{module_name}/") }
        next
    end

    last_release_version = get_lastest_release_version(module_name)
    if last_release_version.nil?
        warn "#{module_name} release history not found.\nIgnore this warning if you add a new module."
        next
    end

    minimum_required_version = next_patch_version(last_release_version)
    current_version = File.read(File.join("#{module_name}", 'version'))
    if Gem::Version.new(minimum_required_version) > Gem::Version.new(current_version)
        warn "#{module_name} version should be greater than the latest release (#{last_release_version})."
    end
}

#
# Check CHANGELOG.md modification
#
# モジュール（versionファイル）の変更がある場合のみCHANGELOG.mdの更新を必須とする
$has_module_changes = $modules.any? { |module_name|
    git.modified_files.include?("#{module_name}/version")
}

if $has_module_changes
    if !git.modified_files.include?("CHANGELOG.md")
        warn "Please update CHANGELOG.md"
    end
end
