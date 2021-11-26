github.dismiss_out_of_range_messages

$diff_files = (git.added_files + git.modified_files + git.deleted_files)
$modules = ["core", "inappmessaging", "notifications", "variables", "visualtracking", "gradle-plugin"]
$formatted_tags = git.tags.map { |tag| tag.strip }

def vup_check(vup_type)    
    format_str = "Version number should be bumped. Run this command:\n`ruby scripts/bump_version.rb %<type>s -t %<module>s`"
    $modules.each { |module_name|
        if !$diff_files.include?("#{module_name}/**")
            next
        end
        
        version = File.read(File.join("#{module_name}", 'version'))
        if !$formatted_tags.include?("#{module_name}-#{version}")
            next
        end

        warn format(format_str, type: vup_type, module: module_name)
    }
end

if github.branch_for_base == "develop" && github.branch_for_head.start_with?("feature/")
    vup_check("minor")
elsif github.branch_for_base == "master" && github.branch_for_head.start_with?("hotfix/")
    vup_check("patch")
end
