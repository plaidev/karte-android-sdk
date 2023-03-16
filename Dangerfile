github.dismiss_out_of_range_messages

$diff_files = (git.added_files + git.modified_files + git.deleted_files)
$modules = ["core", "inappmessaging", "notifications", "variables", "visualtracking", "inbox", "gradle-plugin"]
$formatted_tags = git.tags.map { |tag| tag.strip }

$is_develop_pr = github.branch_for_base == "develop" && github.branch_for_head.start_with?("feature/")
$is_hotfix_pr = (github.branch_for_base == "master" || github.branch_for_base == "develop") && github.branch_for_head.start_with?("hotfix/")

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
# バージョン文字列をバンプアップする
def bump_version(base_version)
    versions = base_version.split('.')
    if $is_develop_pr
        versions[1] = (versions[1].to_i + 1).to_s
        versions[2] = "0"
    elsif $is_hotfix_pr
        versions[2] = (versions[2].to_i + 1).to_s
    end
    versions.join('.')
end

if ($is_develop_pr || $is_hotfix_pr)
    $modules.each { |module_name|
        if !$diff_files.include?("#{module_name}/**")
            next
        end
    
        last_release_version = get_lastest_release_version(module_name)
        if last_release_version.nil?
            warn "#{module_name} release history not found.\nIgnore this warning if you add a new module."
            next
        end
        
        next_version = bump_version(last_release_version)
        current_version = File.read(File.join("#{module_name}", 'version'))
        if Gem::Version.new(next_version) > Gem::Version.new(current_version)
            warn format(
                "Version number should be bumped. Run this command:\n`ruby scripts/bump_version.rb set-version -t %<module>s -n %<version>s`", 
                module: "#{module_name}",
                version: next_version
            )
        end
    }
end

#
# Check CHANGELOG.md modification
#
if ($is_develop_pr || $is_hotfix_pr)
    if !git.modified_files.include?("CHANGELOG.md")
        warn "Please update CHANGELOG.md"
    end    
end
