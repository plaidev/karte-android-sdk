require 'optparse'
require 'tmpdir'
require 'fileutils'


class BaseCommand
  attr_reader :name, :parser, :modules

  def initialize(name)
    @name = name
    @parser = OptionParser.new do |opt|
      define_program_name(opt)
      define_version(opt)
      define_banner(opt)

      opt.separator ''
      opt.separator 'Examples:'
      define_examples(opt)

      opt.separator ''
      opt.separator 'Specific options:'
      define_specific_options(opt)

      opt.separator ''
      opt.separator 'Common options:'
      opt.on_tail('-h', '--help', 'Show help message') do
        show_help
      end
      opt.on_tail('-v', '--version', 'Show program version number') do
        show_version
      end
    end
    @modules = ['core', 'inappmessaging', 'notifications', 'variables', 'visualtracking', 'gradle-plugin']
  end

  def define_program_name(opt)
  end

  def define_version(opt)
  end

  def define_banner(opt)
  end

  def define_examples(opt)
  end

  def define_specific_options(opt)
  end

  def validate()
  end

  def run(argv = ARGV)
    @parser.parse!(argv)
    validate
  end

  def show_help
    puts @parser
    exit
  end

  def show_version
    puts "#{@parser.program_name} #{@parser.version}"
    exit
  end
end

class Command < BaseCommand
  def initialize
    super('bump_version')
    @subcommands = Hash.new do |h, k|
      if k.nil?
        puts @parser
        exit
      else
        @stderr.puts "No such subcommand: #{k}"
        exit 1
      end
    end
  end

  def register_subcommands(subcommands)
    subcommands.each do |subcommand| 
      register_subcommand(subcommand)
    end
  end

  def register_subcommand(subcommand)
    subcommand.parser.program_name = @parser.program_name
    subcommand.parser.version = @parser.version
    @subcommands[subcommand.name] = subcommand
  end

  def define_program_name(opt)
    opt.program_name = @name
  end

  def define_version(opt)
    opt.version = '0.0.1'
  end

  def define_examples(opt)
    opt.separator "    % #{opt.program_name} major -t TARGET"
    opt.separator "    % #{opt.program_name} set-version -t TARGET -n 1.0.0"
    opt.separator "    % #{opt.program_name} current-version -t TARGET"
    opt.separator "    % #{opt.program_name} current-tag -t TARGET"
  end

  def run(argv = ARGV)
    @parser.order!(argv)
    @subcommands[argv.shift].run(argv)
  end
end

class VersionCommand < BaseCommand
  attr_reader :target

  def initialize(name)
    super(name)
  end

  def define_banner(opt)
    opt.banner = "Usage: #{opt.program_name} #{@name} [options]"
  end

  def define_examples(opt)
    opt.separator "    % #{opt.program_name} #{@name} -t TARGET"
  end

  def define_specific_options(opt)
    opt.on('-t VALUE', '--target=VALUE', 'Build target name') { |v| @target = v }
  end

  def get_target
    if @target.nil?
      $stderr.puts "Target is nil."
      exit 1
    else
      if @modules.include?(@target)
        @target
      else
        $stderr.puts "Invalid Target: #{@target}"
        exit 1
      end
    end
  end

  def run(argv = ARGV)
    super(argv)
  end
end

class UpdateVersionCommand < VersionCommand
  def run(argv = ARGV)
    super(argv)
    target = get_target
    update_target_version(target)
  end
end

class BumpVersionCommand < UpdateVersionCommand
  def update_target_version(target)
    version = File.read(File.join("#{target}", 'version'))
    version = bump(version)
    File.write(File.join("#{target}", 'version'), version)
    puts "   [ANDROID_VERSION] Bump #{@name} version for #{target}: #{version}"
  end

  def bump(version)
    version_splits = version.split(".")
    case @name
    when "major"
      version_splits[0] = version_splits[0].to_i + 1
      version_splits[1] = 0
      version_splits[2] = 0
    when "minor"
      version_splits[1] = version_splits[1].to_i + 1
      version_splits[2] = 0
    when "patch"
      version_splits[2] = version_splits[2].to_i + 1
    end
    return version_splits.join(".")
  end
end

class SetVersionCommand < UpdateVersionCommand
  attr_reader :version

  def initialize
    super('set-version')
  end

  def define_specific_options(opt)
    super(opt)
    opt.on('-n VALUE', '--version-number=VALUE', 'Version number') { |v| @version = v }
  end

  def validate
    super
    if @version.nil?
      $stderr.puts '-n or --version-number options are required.'
      exit 1
    end
  end

  def update_target_version(target)
    File.open(File.join("#{target}", 'version'), "w") do |f|
        f.write @version
        puts "   [ANDROID_VERSION] Bump #{@name} version for #{target}: #{@version}"
    end
  end
end

class CurrentVersionCommand < VersionCommand
  def initialize
    super('current-version')
  end

  def run(argv = ARGV)
    super(argv)
    target = get_target

    current_target_version(target)
  end

  def current_target_version(target)
    File.open(File.join("#{target}", 'version'), "r") do |f|
        version = f.read
        puts "   [COCOAPODS_PODSPEC] Current version for #{target}: #{version}"
    end
  end
end

class CurrentTagVersionCommand < VersionCommand
  def initialize
    super('current-tag')
  end

  def run(argv = ARGV)
    super(argv)
    target = get_target

    current_target_tag(target)
  end

  def current_target_tag(target)
    File.open(File.join("#{target}", 'version'), "r") do |f|
        version = f.read
        puts "#{target}-#{version}"
    end
  end
end

command = Command.new
command.register_subcommands [
  BumpVersionCommand.new('major'),
  BumpVersionCommand.new('minor'),
  BumpVersionCommand.new('patch'),
  SetVersionCommand.new,
  CurrentVersionCommand.new,
  CurrentTagVersionCommand.new
]
command.run
