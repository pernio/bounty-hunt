# Bounty hunt
Classic bounty plugin for paper servers.

### Commands <br/>
| Command                         | Perm                       | Description                              |
|---------------------------------|----------------------------|------------------------------------------|
| `/bounty set <player> <amount>` | `bountyhunt.bounty.set`    | Set a bounty on an online player.        |
| `/bounty check <player>`        | `bountyhunt.bounty.check`  | Check if a specific player has a bounty. |
| `/bounty top`                   | `bountyhunt.bounty.top`    | Check the top 10 highest bounties.       |
| `/bounty remove <player>`       | `bountyhunt.bounty.remove` | Remove a bounty from a specific player.  |

### Permissions <br/>
| Perm                       | Default | Description                              |
|----------------------------|---------|------------------------------------------|
| `bountyhunt.bounty`        | True    | Base permission for bounty commands.     |
| `bountyhunt.bounty.set`    | True    | Set a bounty on an online player.        |
| `bountyhunt.bounty.check`  | True    | Check if a specific player has a bounty. |
| `bountyhunt.bounty.top`    | True    | Check the top 10 highest bounties.       |
| `bountyhunt.bounty.remove` | Op      | Remove a bounty from a specific player.  |


### Configuration <br/>
| Perm                      | Default | Description                                          |
|---------------------------|---------|------------------------------------------------------|
| `bounty-tax-percentage`   | 0       | Extra amount requested to use the `set` command.     |
| `bounty-cooldown-seconds` | 60      | Amount of time before a player can set a new bounty. |
| `bounty-minimum-amount`   | 1       | Minimum limit to set a bounty.                       |
| `bounty-max-amount`       | 1000000 | Maximum limit to set a bounty.                       |