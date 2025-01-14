package nu.nerd.df.commands;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.DragonBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.commands.Commands;
import nu.nerd.beastmaster.commands.ExecutorBase;
import nu.nerd.df.DragonFight;
import nu.nerd.df.DragonUtil;
import nu.nerd.df.Stage;

// ----------------------------------------------------------------------------
/**
 * Command executor for the <i>/df</i> command.
 */
public class DFExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public DFExecutor() {
        super("df", "help",
              "info", "stop", "next", "stage", "owner", "unclaimed",
              "list", "swap", "move", "config");
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

        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            DragonFight.FIGHT.cmdInfo(sender);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            DragonFight.FIGHT.cmdStop(sender);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("next")) {
            DragonFight.FIGHT.cmdNextStage(sender);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("stage")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /df stage <number>");
                sender.sendMessage(ChatColor.RED + "Skip forward to the specified stage, from 0 to 11 (dragon only).");
                return true;
            }
            String numberArg = args[1];
            Integer stageNumber = Commands.parseNumber(numberArg, Commands::parseInt,
                                                       n -> n >= 0 && n <= 11,
                                                       () -> sender.sendMessage(ChatColor.RED + "The stage number must be an integer from 1 to 11."),
                                                       null);
            if (stageNumber == null) {
                return true;
            }

            DragonFight.FIGHT.cmdSkipToStage(sender, stageNumber);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("owner")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /df owner <player>");
                sender.sendMessage(ChatColor.RED + "Set the owner of the current fight.");
                return true;
            }
            String playerArg = args[1];

            // Disallow if the dragon has not yet spawned.
            DragonBattle battle = DragonUtil.getFightWorld().getEnderDragonBattle();
            if (battle.getEnderDragon() == null) {
                sender.sendMessage(ChatColor.RED + "The fight owner cannot be changed until the dragon is spawned.");
                return true;
            }

            // Always returns a non-null OfflinePlayer.
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerArg);
            DragonFight.CONFIG.FIGHT_OWNER = offlinePlayer.getUniqueId();
            DragonFight.CONFIG.save();

            sender.sendMessage(ChatColor.DARK_PURPLE + "The fight owner was set to " +
                               ChatColor.LIGHT_PURPLE + offlinePlayer.getName() +
                               ChatColor.DARK_PURPLE + ", uuid " +
                               ChatColor.LIGHT_PURPLE + offlinePlayer.getUniqueId().toString() +
                               ChatColor.DARK_PURPLE + ".");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("unclaimed")) {
            if (DragonFight.CONFIG.UNCLAIMED_PRIZES.isEmpty()) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "No players have unclaimed prizes.");
                return true;
            }

            sender.sendMessage(ChatColor.DARK_PURPLE + "The following players have unclaimed prizes:");
            String players = DragonFight.CONFIG.UNCLAIMED_PRIZES.entrySet().stream()
                .map(entry -> {
                    // OfflinePlayer never null:
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                    String playerName = (offlinePlayer.getName() == null) ? offlinePlayer.getUniqueId().toString()
                                                                          : offlinePlayer.getName();
                    return ChatColor.LIGHT_PURPLE + playerName + ChatColor.DARK_PURPLE + ": " +
                           ChatColor.WHITE + Integer.toString(entry.getValue());
                })
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(ChatColor.DARK_PURPLE + ", "));
            sender.sendMessage(players);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "Stages:");
            for (int stageNumber = 1; stageNumber <= 11; ++stageNumber) {
                Stage stage = DragonFight.CONFIG.getStage(stageNumber);
                sender.sendMessage(ChatColor.DARK_PURPLE + "(" + stageNumber + ") " +
                                   ChatColor.WHITE + stage.format(stage.getTitle()) +
                                   ChatColor.WHITE + BAR_COLORS[stage.getBarColor().ordinal()] + " ███ " +
                                   ChatColor.WHITE + stage.format(stage.getSubtitle()));
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("swap")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /df swap <from> <to>");
                sender.sendMessage(ChatColor.RED + "Swap two stages by stage number (1 to 10).");
                return true;
            }
            Integer fromNumber = Commands.parseNumber(args[1],
                                                      Commands::parseInt,
                                                      n -> n >= 1 && n <= 10,
                                                      () -> sender.sendMessage(ChatColor.RED + "<from> must be a stage number from 1 to 10."),
                                                      null);
            Integer toNumber = Commands.parseNumber(args[2],
                                                    Commands::parseInt,
                                                    n -> n >= 1 && n <= 10,
                                                    () -> sender.sendMessage(ChatColor.RED + "<to> must be a stage number from 1 to 10."),
                                                    null);
            if (fromNumber == null || toNumber == null) {
                return true;
            }

            Stage.swap(DragonFight.CONFIG.getStage(fromNumber),
                       DragonFight.CONFIG.getStage(toNumber));
            DragonFight.CONFIG.save();
            BeastMaster.CONFIG.save();
            sender.sendMessage(ChatColor.DARK_PURPLE + "Stages " +
                               ChatColor.LIGHT_PURPLE + fromNumber +
                               ChatColor.DARK_PURPLE + " and " +
                               ChatColor.LIGHT_PURPLE + toNumber +
                               ChatColor.DARK_PURPLE + " swapped.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("move")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /df move <from> <to>");
                sender.sendMessage(ChatColor.RED + "Move stage <from> to stage <to> and shift in-between stages into the gap.");
                return true;
            }

            Integer fromNumber = Commands.parseNumber(args[1],
                                                      Commands::parseInt,
                                                      n -> n >= 1 && n <= 10,
                                                      () -> sender.sendMessage(ChatColor.RED + "<from> must be a stage number from 1 to 10."),
                                                      null);
            Integer toNumber = Commands.parseNumber(args[2],
                                                    Commands::parseInt,
                                                    n -> n >= 1 && n <= 10,
                                                    () -> sender.sendMessage(ChatColor.RED + "<to> must be a stage number from 1 to 10."),
                                                    null);
            if (fromNumber == null || toNumber == null) {
                return true;
            }

            if (fromNumber == toNumber) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "That's the situation already.");
                return true;
            }

            // So by here, we know that the from and to numbers differ.
            Stage.swap(DragonFight.CONFIG.getStage(fromNumber),
                       DragonFight.CONFIG.getStage(toNumber));

            // To fix the order, swap as many times as there are stages between.
            if (fromNumber < toNumber) {
                for (int stage = fromNumber; stage < toNumber - 1; ++stage) {
                    Stage.swap(DragonFight.CONFIG.getStage(stage),
                               DragonFight.CONFIG.getStage(stage + 1));
                }
            } else {
                // fromNumber > toNumber
                for (int stage = fromNumber; stage > toNumber + 1; --stage) {
                    Stage.swap(DragonFight.CONFIG.getStage(stage),
                               DragonFight.CONFIG.getStage(stage - 1));
                }
            }
            sender.sendMessage(ChatColor.DARK_PURPLE + "Moved stage " +
                               ChatColor.LIGHT_PURPLE + fromNumber +
                               ChatColor.DARK_PURPLE + " to position " +
                               ChatColor.LIGHT_PURPLE + toNumber +
                               ChatColor.DARK_PURPLE + ".");
            DragonFight.CONFIG.save();
            BeastMaster.CONFIG.save();
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("config")) {
            if (args.length == 1) {
                configUsage(sender);
                return true;
            }

            Integer stageNumber = Commands.parseNumber(args[1],
                                                       Commands::parseInt,
                                                       n -> n >= 1 && n <= 11,
                                                       () -> sender.sendMessage(ChatColor.RED + "The stage must be a number from 1 to 10."),
                                                       null);
            if (stageNumber == null) {
                configUsage(sender);
                return true;
            }

            Stage stage = DragonFight.CONFIG.getStage(stageNumber);
            if (args.length == 2) {
                // Show stage configuration.
                sender.sendMessage(ChatColor.DARK_PURPLE + "Stage: " + ChatColor.LIGHT_PURPLE + stageNumber);
                sender.sendMessage(ChatColor.DARK_PURPLE + "barcolor: " + BAR_COLORS[stage.getBarColor().ordinal()] + stage.getBarColor());
                sender.sendMessage(ChatColor.DARK_PURPLE + "title (unformatted): " + ChatColor.WHITE + stage.getTitle());
                sender.sendMessage(ChatColor.DARK_PURPLE + "title (formatted): " + ChatColor.WHITE + stage.format(stage.getTitle()));
                sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (unformatted): " + ChatColor.WHITE + stage.getSubtitle());
                sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (formatted): " + ChatColor.WHITE + stage.format(stage.getSubtitle()));
                sender.sendMessage(ChatColor.DARK_PURPLE + "message (unformatted): " + ChatColor.WHITE + stage.getMessage());
                sender.sendMessage(ChatColor.DARK_PURPLE + "message (formatted): " + ChatColor.WHITE + stage.format(stage.getMessage()));
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
                                               ChatColor.DARK_PURPLE + " bar color: " + BAR_COLORS[barColor.ordinal()] + barColor);
                        } catch (IllegalArgumentException ex) {
                            sender.sendMessage(ChatColor.RED + "Invalid bar color name.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /df config <stage> barcolor <color>");
                    }
                    if (listColors) {
                        sender.sendMessage(ChatColor.RED + "Colors: " +
                                           Stream.of(BarColor.values())
                                               .map(c -> BAR_COLORS[c.ordinal()] + c.toString())
                                               .collect(Collectors.joining(" ")));
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
                            sender.sendMessage(ChatColor.DARK_PURPLE + "title (formatted): " + ChatColor.WHITE + stage.format(stage.getTitle()));
                        }

                    } else if (propertyArg.equalsIgnoreCase("subtitle")) {
                        stage.setSubtitle(text);
                        DragonFight.CONFIG.save();
                        sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (unformatted): " + ChatColor.WHITE + stage.getSubtitle());
                        sender.sendMessage(ChatColor.DARK_PURPLE + "subtitle (formatted): " + ChatColor.WHITE + stage.format(stage.getSubtitle()));

                    } else if (propertyArg.equalsIgnoreCase("message")) {
                        stage.setMessage(text);
                        DragonFight.CONFIG.save();
                        sender.sendMessage(ChatColor.DARK_PURPLE + "message (unformatted): " + ChatColor.WHITE + stage.getMessage());
                        sender.sendMessage(ChatColor.DARK_PURPLE + "message (formatted): " + ChatColor.WHITE + stage.format(stage.getMessage()));

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
     * Look up table from BarColor ordinal to the corresponding chat colour.
     */
    private static final ChatColor[] BAR_COLORS = {
        ChatColor.LIGHT_PURPLE, // PINK
        ChatColor.DARK_AQUA, // BLUE
        ChatColor.RED, // RED
        ChatColor.GREEN, // GREEN
        ChatColor.YELLOW, // YELLOW,
        ChatColor.DARK_PURPLE, // PURPLE
        ChatColor.WHITE // WHITE
    };
} // class DFExecutor