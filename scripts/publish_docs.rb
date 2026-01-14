require 'optparse'
require 'tmpdir'
require 'fileutils'
require 'set'

class Git
  attr_reader :workspace

  def initialize(repository, workspace)
    @repository = repository
    @workspace = File.join(workspace, 'repository')
    @branch = 'master'
  end

  def clone
    system("git clone -b #{@branch} #{@repository} #{@workspace}")
  end

  def checkout(branch_name)
    Dir.chdir(@workspace) do |dir|
      system("git checkout -b #{branch_name}")
      @branch = branch_name
    end
  end

  def add
    Dir.chdir(@workspace) do |dir|
      system("git add .")
    end
  end

  def commit(message)
    Dir.chdir(@workspace) do |dir|
      system("git commit -m '#{message}'")
    end
  end

  def push
    Dir.chdir(@workspace) do |dir|
      system("git push origin #{@branch}")
    end
  end
end

class Documents
  attr_reader :module_name, :platform, :version

  def initialize(module_name, platform, workspace)
    @module_name = module_name
    @platform = platform
    @workspace = workspace
  end

  def publish(force)
    force ||= false

    @version = File.open(File.join(@module_name, "version"), "r") { |f| f.read.strip }
    ver_docs_dir = _get_version_dir
    if Dir.exist?(ver_docs_dir) and !force
      puts "#{@module_name} #{@version} documents already exist."
      return false
    end

    _copy_version_docs

    # Copy common resources file to version directory
    _copy_common_resources(ver_docs_dir, false)

    # Copy common resources file to latest directory
    latest_docs_dir = _copy_latest_docs
    if latest_docs_dir
      _copy_common_resources(latest_docs_dir, true)
    end

    # Copy common resources to module root directory
    # Required as entry point for search and navigation features
    module_dir = _get_module_dir
    _copy_common_resources_to_module_root(module_dir)

    return true
  end

  # Copy module contents to version_dir/module_name sub directory
  def _copy_version_docs
    ver_docs_dir = _get_version_dir
    if Dir.exist?(ver_docs_dir)
      FileUtils.rm_rf([ver_docs_dir])
    end
    FileUtils.mkdir_p(ver_docs_dir)

    FileUtils.cp_r(File.join(_get_generated_docs_dir, "."), ver_docs_dir)
    version_file = File.join(ver_docs_dir, 'version')
    system("echo #{@version} > #{version_file}")
  end

  # Copy module contents to latest_dir/module_name sub directory
  def _copy_latest_docs
    is_latest = true
    latest_docs_dir = _get_latest_dir
    if Dir.exist?(latest_docs_dir)
      version = _get_latest_document_version
      if Gem::Version.new(@version) >= Gem::Version.new(version)
        FileUtils.rm_rf([latest_docs_dir])
      else
        is_latest = false
      end
    end
    if is_latest
      FileUtils.mkdir_p(latest_docs_dir)

      FileUtils.cp_r(File.join(_get_generated_docs_dir, "."), latest_docs_dir)

      version_file = File.join(latest_docs_dir, 'version')
      system("echo #{@version} > #{version_file}")
      return latest_docs_dir
    end
    return nil
  end

  def _get_latest_document_version
    path = File.join(_get_latest_dir, 'version')
    if File.exist?(path)
      `cat #{path}`
    else
      nil
    end
  end

  def _get_generated_docs_dir
    File.join("build", "dokka", "htmlMultiModule", @module_name)
  end

  def _get_latest_dir
    File.join(_get_platform_dir, @module_name, 'latest')
  end

  def _get_version_dir
    File.join(_get_module_dir, @version)
  end

  def _get_module_dir
    File.join(_get_platform_dir, @module_name)
  end

  def _get_platform_dir
    File.join(@workspace, 'repository', 'docs', @platform)
  end

  # Copy basic resources (images, styles)
  def _copy_basic_resources(target_dir)
    source_root = File.join("build", "dokka", "htmlMultiModule")

    ['images', 'styles'].each do |resource|
      source_path = File.join(source_root, resource)
      target_path = File.join(target_dir, resource)
      if Dir.exist?(source_path)
        if Dir.exist?(target_path)
          FileUtils.rm_rf([target_path])
        end
        FileUtils.cp_r(source_path, target_path)
        puts "Copied common resource '#{resource}' to #{target_dir}"
      end
    end
  end

  # Copy scripts directory and rewrite files
  def _copy_scripts_directory(target_dir, &rewrite_pages_block)
    source_root = File.join("build", "dokka", "htmlMultiModule")
    scripts_source = File.join(source_root, 'scripts')
    scripts_target = File.join(target_dir, 'scripts')

    if Dir.exist?(scripts_source)
      if Dir.exist?(scripts_target)
        FileUtils.rm_rf([scripts_target])
      end
      FileUtils.cp_r(scripts_source, scripts_target)

      # Rewrite pages.json
      pages_json_path = File.join(scripts_target, 'pages.json')
      if File.exist?(pages_json_path) && rewrite_pages_block
        rewrite_pages_block.call(pages_json_path)
      end

      # Rewrite sourceset_dependencies.js
      sourceset_dependencies_path = File.join(scripts_target, 'sourceset_dependencies.js')
      if File.exist?(sourceset_dependencies_path)
        _rewrite_sourceset_dependencies(sourceset_dependencies_path)
      end

      puts "Copied common resource 'scripts' to #{target_dir}"
    end
  end

  # Copy navigation.html and rewrite content
  def _copy_navigation_html(target_dir, &rewrite_content_block)
    navigation_source = File.join(_get_generated_docs_dir, 'navigation.html')
    navigation_target = File.join(target_dir, 'navigation.html')

    if File.exist?(navigation_source)
      content = File.read(navigation_source)

      # Rewrite content (use block if provided)
      content = rewrite_content_block.call(content) if rewrite_content_block

      File.write(navigation_target, content)
      puts "Copied and modified navigation.html to #{target_dir}"
    end
  end

  # Copy common resources to target directory
  def _copy_common_resources(target_dir, is_latest = false)
    # Copy basic resources (images, styles)
    _copy_basic_resources(target_dir)

    # Copy and modify scripts directory
    _copy_scripts_directory(target_dir) do |pages_json_path|
      _rewrite_pages_json(pages_json_path) if is_latest
    end

    # Copy and modify navigation.html
    _copy_navigation_html(target_dir) do |content|
      if is_latest
        _rewrite_navigation_links_for_latest(content)
      else
        _remove_other_module_links(content)
      end
    end

    # Rewrite library-name and library-version in module's HTML files
    _rewrite_library_name_and_version(target_dir)

    # Rewrite resource paths in HTML files
    _rewrite_resource_paths(target_dir)
  end

  # Read and parse pages.json
  def _rewrite_pages_json(pages_json_path)
    require 'json'

    content = File.read(pages_json_path)
    pages = JSON.parse(content)

    # Filter and rewrite paths for all modules
    filtered_pages = pages.select do |page|
      location = page['location']
      next false unless location

      # Check which module this page belongs to
      if location =~ /^([^\/]+)\//
        module_name = $1
        # Exclude inbox module only
        if module_name == 'inbox'
          false
        elsif module_name == @module_name
          # Current module : core/path -> path
          page['location'] = location.sub(/^#{@module_name}\//, '')
          true
        else
          # Other module : debugger/path -> ../../debugger/latest/path
          path_without_module = location.sub(/^#{module_name}\//, '')
          page['location'] = "../../#{module_name}/latest/#{path_without_module}"
          true
        end
      else
        # Keep pages without module prefix
        true
      end
    end

    # Write back
    File.write(pages_json_path, JSON.generate(filtered_pages))
    puts "Rewrote pages.json for module: #{@module_name} (#{filtered_pages.length} pages)"
  end

  # Read the sourceset_dependencies.js file
  def _rewrite_sourceset_dependencies(sourceset_dependencies_path)
    require 'json'

    content = File.read(sourceset_dependencies_path)

    # Extract the JSON object from the JavaScript variable assignment
    if content =~ /sourceset_dependencies\s*=\s*'(.*)'/m
      json_str = $1
      sourcesets = JSON.parse(json_str)

      # Find actually used sourcesets by scanning HTML files
      used_sourcesets = _find_used_sourcesets

      # Filter to keep only current module's sourcesets that are actually used
      filtered_sourcesets = sourcesets.select do |key, _|
        key =~ /^:#{@module_name}:/ && used_sourcesets.include?(key)
      end

      # Write back in the same format
      new_json_str = JSON.generate(filtered_sourcesets)
      new_content = "sourceset_dependencies = '#{new_json_str}'"
      File.write(sourceset_dependencies_path, new_content)
      puts "Rewrote sourceset_dependencies.js for module: #{@module_name} (#{filtered_sourcesets.length} sourcesets)"
    else
      puts "Warning: Could not parse sourceset_dependencies.js"
    end
  end

  # Find actually used sourcesets
  def _find_used_sourcesets
    used_sourcesets = Set.new
    module_dir = _get_generated_docs_dir

    # Scan all HTML files in the module directory
    Dir.glob(File.join(module_dir, '**', '*.html')).each do |html_file|
      content = File.read(html_file)

      # Extract data-filter and data-togglable attributes
      content.scan(/data-(?:filter|togglable)="([^"]+)"/) do |match|
        sourceset = match[0]
        used_sourcesets.add(sourceset) if sourceset =~ /^:#{@module_name}:/
      end
    end

    used_sourcesets
  end

  # Rewrite links for latest directory
  def _rewrite_navigation_links_for_latest(content)
    result = content.dup

    # Remove inbox section (inbox is not published)
    result = _remove_inbox_section(result)

    # Rewrite all module links : ../<module>/ -> appropriate path
    result = result.gsub(/href="\.\.\/([^\/\"]+)\//) do |match|
      module_name = $1
      if module_name =~ /^(images|scripts|styles)$/
        # Resource links: ../images/ -> images/
        "href=\"#{module_name}/"
      elsif module_name == @module_name
        # Current module : ../core/ -> core/
        "href=\"#{module_name}/"
      else
        # Other modules : ../debugger/ -> ../../debugger/latest/
        "href=\"../../#{module_name}/latest/"
      end
    end

    result
  end

  # Remove inbox section (inbox is not published)
  def _remove_inbox_section(content)
    lines = content.split("\n")
    result_lines = []
    in_inbox_section = false
    depth = 0
    initial_depth = 0

    lines.each do |line|
      # Detect inbox section start
      if line =~ /id="inbox-nav-submenu/
        in_inbox_section = true
        # Count opening divs on this line to set initial depth
        initial_depth = line.scan(/<div/).length
        depth = initial_depth
        next
      end

      if in_inbox_section
        # Track div depth
        depth += line.scan(/<div/).length
        depth -= line.scan(/<\/div>/).length

        # Reset when we've closed all divs from the inbox section
        if depth < initial_depth
          in_inbox_section = false
          initial_depth = 0
        else
          next
        end
      end

      result_lines << line
    end

    result_lines.join("\n")
  end

  # Remove navigation sections for other modules, keep only current module
  def _remove_other_module_links(content)
    doc = content.dup

    # Pattern to match module navigation sections
    modules_pattern = /(core|inappmessaging|notifications|variables|visualtracking|inappframe|debugger|inbox)/

    # Split by module sections and keep only current module's section
    lines = doc.split("\n")
    result_lines = []
    current_module = nil
    depth = 0
    skip = false

    lines.each do |line|
      # Detect module section start
      if line =~ /id="(#{modules_pattern})-nav-submenu"/
        current_module = $1
        skip = (current_module != @module_name)
        depth = 0
      end

      # Track div depth
      depth += line.scan(/<div/).length
      depth -= line.scan(/<\/div>/).length

      # Keep line if not skipping
      result_lines << line unless skip

      # Reset skip when section ends
      if skip && depth <= 0 && current_module
        skip = false
        current_module = nil
      end
    end

    result = result_lines.join("\n")

    # Remove ../ prefix
    result = result.gsub(/href="\.\.\/#{@module_name}\//, "href=\"#{@module_name}/")

    result
  end

  # Rewrite library-name and library-version in all HTML files
  def _rewrite_library_name_and_version(target_dir)
    return unless Dir.exist?(target_dir)

    # Capitalize module name for display
    display_name = @module_name.capitalize

    # Scan all HTML files in the target directory
    html_files = Dir.glob(File.join(target_dir, '**', '*.html'))

    html_files.each do |html_file|
      content = File.read(html_file)
      modified = false

      # Replace library-name: "Karte" -> module name
      if content =~ /<div class="library-name">/
        new_content = content.gsub(
          /(<div class="library-name">.*?>)\s*Karte\s*(<\/a>)/m,
          "\\1\n                            #{display_name}\n                    \\2"
        )
        if new_content != content
          content = new_content
          modified = true
        end
      end

      # Add library-version if it's empty
      if content =~ /<div class="library-version">\s*<\/div>/
        new_content = content.gsub(
          /<div class="library-version">\s*<\/div>/,
          "<div class=\"library-version\">\n            #{@version}\n            </div>"
        )
        if new_content != content
          content = new_content
          modified = true
        end
      end

      # Write back if modified
      if modified
        File.write(html_file, content)
      end
    end

    puts "Rewrote library-name to '#{display_name}' and library-version to '#{@version}' in #{html_files.length} HTML files"
  end

  # Rewrite resource paths in HTML files (remove ../ prefix from resource links)
  def _rewrite_resource_paths(target_dir)
    return unless Dir.exist?(target_dir)

    # Scan all HTML files in the target directory
    html_files = Dir.glob(File.join(target_dir, '**', '*.html'))

    html_files.each do |html_file|
      content = File.read(html_file)

      # Rewrite resource paths : ../<resource>/ -> <resource>/
      new_content = content.gsub(/(href|src)="\.\.\/(images|scripts|styles)\//, '\1="\2/')

      if new_content != content
        File.write(html_file, new_content)
      end
    end

    puts "Rewrote resource paths in #{html_files.length} HTML files"
  end

  # Copy common resources to module root directory
  def _copy_common_resources_to_module_root(module_dir)
    return unless Dir.exist?(module_dir)

    # Copy basic resources (images, styles)
    _copy_basic_resources(module_dir)

    # Copy and modify scripts directory
    _copy_scripts_directory(module_dir) do |pages_json_path|
      _rewrite_pages_json_for_module_root(pages_json_path)
    end

    # Copy and modify navigation.html
    _copy_navigation_html(module_dir) do |content|
      _rewrite_navigation_links_for_module_root(content)
    end

    # Create index.html that redirects to latest
    index_html_content = <<~HTML
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta http-equiv="refresh" content="0; url=latest/index.html">
        <script>window.location.href = "latest/index.html";</script>
      </head>
      <body>
        <p>Redirecting to <a href="latest/index.html">latest documentation</a>...</p>
      </body>
      </html>
    HTML
    index_html_path = File.join(module_dir, 'index.html')
    File.write(index_html_path, index_html_content)
    puts "Created redirect index.html in module root #{module_dir}"
  end

  # Rewrite pages.json for module root
  def _rewrite_pages_json_for_module_root(pages_json_path)
    require 'json'

    content = File.read(pages_json_path)
    pages = JSON.parse(content)

    # Filter and rewrite paths for all modules (exclude inbox only)
    filtered_pages = pages.select do |page|
      location = page['location']
      next false unless location

      # Check which module this page belongs to
      if location =~ /^([^\/]+)\//
        module_name = $1
        # Exclude inbox module only
        if module_name == 'inbox'
          false
        elsif module_name == @module_name
          # Current module : core/path -> latest/path
          path_without_module = location.sub(/^#{@module_name}\//, '')
          page['location'] = "latest/#{path_without_module}"
          true
        else
          # Other modules : debugger/path -> ../debugger/latest/path
          path_without_module = location.sub(/^#{module_name}\//, '')
          page['location'] = "../#{module_name}/latest/#{path_without_module}"
          true
        end
      else
        # Keep pages without module prefix and add latest/ prefix
        page['location'] = "latest/#{location}"
        true
      end
    end

    # Write back
    File.write(pages_json_path, JSON.generate(filtered_pages))
    puts "Rewrote pages.json for module root: #{@module_name} (#{filtered_pages.length} pages)"
  end

  # Rewrite navigation links for module root
  def _rewrite_navigation_links_for_module_root(content)
    result = content.dup

    # Remove inbox section (inbox is not published)
    result = _remove_inbox_section(result)

    # Rewrite all module links to point to latest/
    result = result.gsub(/href="\.\.\/([^\/\"]+)\//) do |match|
      module_name = $1
      if module_name =~ /^(images|scripts|styles)$/
        # Resource links : ../images/ -> images/
        "href=\"#{module_name}/"
      elsif module_name == @module_name
        # Current module : ../core/ -> latest/
        "href=\"latest/"
      else
        # Other modules : ../debugger/ -> ../debugger/latest/
        "href=\"../#{module_name}/latest/"
      end
    end

    result
  end

end


class Command
  attr_reader :force, :branch

  def self.release_proc(dir)
    proc {
      puts "Remove workspace => #{dir}"
      FileUtils.rm_rf([dir])
    }
  end

  def initialize
    @parser = OptionParser.new do |opt|
      opt.program_name = 'publish_docs'
      opt.version = '0.0.1'
      opt.banner = "Usage: #{opt.program_name} [options]"

      opt.separator ''
      opt.separator 'Examples:'
      opt.separator "    % #{opt.program_name} [-f] [--branch BRANCH_NAME]"

      opt.separator ''
      opt.separator 'Specific options:'
      opt.on('-f', '--force', 'Force override docs') { |v| @force = v }
      opt.on('-b', '--branch BRANCH', 'Branch name to publish docs to (default: master)') { |v| @branch = v }

      opt.separator ''
      opt.separator 'Common options:'
      opt.on_tail('-h', '--help', 'Show help message') do
        puts opt
        exit
      end
      opt.on_tail('-v', '--version', 'Show program version number') do
        puts "#{opt.program_name} #{opt.version}"
        exit
      end
    end
    @parser.parse!(ARGV)
    @workspace = Dir.mktmpdir

    ObjectSpace.define_finalizer(self, self.class.release_proc(@workspace))
  end

  def run
    git = Git.new('https://github.com/plaidev/karte-sdk-docs.git', @workspace)
    unless git.clone
      $stderr.puts 'Failed to clone git repository.'
      exit 1
    end

    # Checkout to the specified branch if provided
    git.checkout(@branch) if @branch

    docs_dir = File.join(git.workspace, 'docs', 'android')
    FileUtils.mkdir_p(docs_dir)
    modules = ['core', 'inappmessaging', 'notifications', 'variables', 'visualtracking', 'inappframe', 'debugger']
    modules.each do |mod|
      docs = Documents.new(mod, 'android', @workspace)
      if docs.publish(@force)
        git.add
        git.commit("[android] Add #{mod} #{docs.version} documents")
        git.push
      end
    end
  end
end

command = Command.new
command.run
