package nu.nerd.df.commands;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.commands.Commands;
import nu.nerd.beastmaster.commands.ExecutorBase;
import nu.nerd.df.DragonFight;
import nu.nerd.df.Stage;

// ----------------------------------------------------------------------------
/**
 * Command executor for the `/df` command.
 */
public class DFExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * @param name
     * @param subcommands
     */
    public DFExecutor() {
        super("df", "help", "stop", "next", "config");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            DragonFight.FIGHT.stop(sender);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("next")) {
            DragonFight.FIGHT.nextStage(sender);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("config")) {
            if (args.length == 1) {
                configUsage(sender);
                return true;
            }

            Integer stageNumber = Commands.parseNumber(args[1],
                                                       Commands::parseInt,
                                                       n -> n >= 1 && n <= 10,
                                                       null,
                                                       () -> sender.sendMessage(ChatColor.RED + "The stage must be a number from 1 to 10."));
            if (stageNumber == null) {
                configUsage(sender);
                return true;
            }

            Stage stage = DragonFight.CONFIG.getStage(stageNumber);
            if (args.length == 2) {
                // Show stage configuration.
                sender.sendMessage(ChatColor.DARK_PURPLE + "Stage: " + ChatColor.LIGHT_PURPLE + stageNumber);
                sender.sendMessage(ChatColor.DARK_PURPLE + "barcolor: " + BAR_COLOR_NAMES[stage.getBarColor().ordinal()]);
                sender.sendMessage(ChatColor.DARK_PURPLE + "title (unformatted): " + ChatColor.WHITE + stage.getTitle());
                sender.sendMessage(ChatColor.DARK_PURPLE + "title (formatted): " + stage.format(stage.getTitle()));
                sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (unformatted): " + ChatColor.WHITE + stage.getSubtitle());
                sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (formatted): " + stage.format(stage.getSubtitle()));
                sender.sendMessage(ChatColor.DARK_PURPLE + "message (unformatted): " + ChatColor.WHITE + stage.getMessage());
                sender.sendMessage(ChatColor.DARK_PURPLE + "message (formatted): " + stage.format(stage.getMessage()));
                return true;
            } else if (args.length >= 3) {
                String propertyArg = args[2];
                if (propertyArg.equalsIgnoreCase("barcolor")) {
                    boolean listColors = true;
                    if (args.length == 4) {
                        try {
                            BarColor barColor = BarColor.valueOf(args[3].toUpperCase());
                            stage.setBarColor(barColor);
                            DragonFight.CONFIG.save();
                            listColors = false;
                            sender.sendMessage(ChatColor.DARK_PURPLE + "Stage " +
                                               ChatColor.LIGHT_PURPLE + stage.getStageNumber() +
                                               ChatColor.DARK_PURPLE + " bar color: " + BAR_COLOR_NAMES[barColor.ordinal()]);
                        } catch (IllegalArgumentException ex) {
                            sender.sendMessage(ChatColor.RED + "Invalid bar color name.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /df config <stage> barcolor <color>");
                    }
                    if (listColors) {
                        sender.sendMessage(ChatColor.RED + "Colors: " + Stream.of(BAR_COLOR_NAMES).collect(Collectors.joining(" ")));
                    }

                } else {
                    String[] textArgs = Arrays.copyOfRange(args, 3, args.length);
                    String text = Stream.of(textArgs).collect(Collectors.joining(" "));
                    if (propertyArg.equalsIgnoreCase("title")) {
                        if (text.isEmpty()) {
                            sender.sendMessage(ChatColor.RED + "The title text can't be empty (subtitles wouldn't show).");
                            sender.sendMessage(ChatColor.RED + "Usage: /df config <stage> title <text>");
                        } else {
                            stage.setTitle(text);
                            DragonFight.CONFIG.save();
                            sender.sendMessage(ChatColor.DARK_PURPLE + "Stage: " + ChatColor.LIGHT_PURPLE + stageNumber);
                            sender.sendMessage(ChatColor.DARK_PURPLE + "title (unformatted): " + ChatColor.WHITE + stage.getTitle());
                            sender.sendMessage(ChatColor.DARK_PURPLE + "title (formatted): " + stage.format(stage.getTitle()));
                        }

                    } else if (propertyArg.equalsIgnoreCase("subtitle")) {
                        stage.setSubtitle(text);
                        DragonFight.CONFIG.save();
                        sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (unformatted): " + ChatColor.WHITE + stage.getSubtitle());
                        sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (formatted): " + stage.format(stage.getSubtitle()));

                    } else if (propertyArg.equalsIgnoreCase("message")) {
                        stage.setMessage(text);
                        DragonFight.CONFIG.save();
                        sender.sendMessage(ChatColor.DARK_PURPLE + "message (unformatted): " + ChatColor.WHITE + stage.getMessage());
                        sender.sendMessage(ChatColor.DARK_PURPLE + "message (formatted): " + stage.format(stage.getMessage()));

                    } else {
                        configUsage(sender);
                    }
                }
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Invalid arguments. Try /" + command.getName() + " help.");
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Show usage message.
     * 
     * @param sender the message sender.
     */
    protected void configUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.RED + "  /df config <stage>");
        sender.sendMessage(ChatColor.RED + "  /df config <stage> barcolor <color>");
        sender.sendMessage(ChatColor.RED + "  /df config <stage> title <text>");
        sender.sendMessage(ChatColor.RED + "  /df config <stage> subtitle <text>");
        sender.sendMessage(ChatColor.RED + "  /df config <stage> message <text>");
        sender.sendMessage(ChatColor.RED + "{} in <text> is replaced by the stage number.");
    }

    // ------------------------------------------------------------------------
    /**
     * Look up table from BarColor ordinal to the corresponding colour name, in
     * the corresponding chat colour.
     */
    private static final String[] BAR_COLOR_NAMES = {
        ChatColor.LIGHT_PURPLE + "PINK",
        ChatColor.DARK_AQUA + "BLUE",
        ChatColor.RED + "RED",
        ChatColor.GREEN + "GREEN",
        ChatColor.YELLOW + "YELLOW",
        ChatColor.DARK_PURPLE + "PURPLE",
        ChatColor.WHITE + "WHITE"
    };
} // class DFExecutor