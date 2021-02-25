require 'optparse'
require 'tmpdir'
require 'fileutils'

class Git
  attr_reader :workspace

  def initialize(repository, workspace)
    @repository = repository
    @workspace = File.join(workspace, 'repository')
    @branch = 'master'
  end

  def clone
    system("git clone #{@repository} #{@workspace}")
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
    _copy_latest_docs
    return true
  end

  def _copy_version_docs
    module_docs_dir = _get_module_dir
    ver_docs_dir = _get_version_dir
    if Dir.exist?(ver_docs_dir)
      FileUtils.rm_rf([ver_docs_dir])
    end
    FileUtils.mkdir_p(module_docs_dir)
    FileUtils.cp_r(_get_generated_docs_dir, ver_docs_dir)
    version_file = File.join(ver_docs_dir, 'version')
    system("echo #{@version} > #{version_file}")
  end

  def _copy_latest_docs
    module_docs_dir = _get_module_dir
    latest_docs_dir = _get_latest_dir
    if Dir.exist?(latest_docs_dir)
      version = _get_latest_document_version
      if Gem::Version.new(@version) >= Gem::Version.new(version)
        FileUtils.rm_rf([latest_docs_dir])
      end
    end
    FileUtils.mkdir_p(module_docs_dir)
    FileUtils.cp_r(_get_generated_docs_dir, latest_docs_dir)
    version_file = File.join(latest_docs_dir, 'version')
    system("echo #{@version} > #{version_file}")
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
    File.join(@module_name, "build", "dokka", "html")
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
end


class Command
  attr_reader :force

  def self.release_proc(dir)
    proc {
      puts "Remove workspace => #{dir}"
      FileUtils.rm_rf([dir])
    }
  end

  def initialize
    @parser = OptionParser.new do |opt|
      opt.program_name = 'generate_docs'
      opt.version = '0.0.1'
      opt.banner = "Usage: #{opt.program_name} [options]"

      opt.separator ''
      opt.separator 'Examples:'
      opt.separator "    % #{opt.program_name} [-f]"

      opt.separator ''
      opt.separator 'Specific options:'
      opt.on('-f', '--force', 'Force override docs') { |v| @force = v }

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

    docs_dir = File.join(git.workspace, 'docs', 'android')
    FileUtils.mkdir_p(docs_dir)
    modules = ['core', 'inappmessaging', 'notifications', 'variables', 'visualtracking']
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
