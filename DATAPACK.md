# Data Pack Quest Format

## Modifying the default list

Adding quest targets to the default list:
- Create a file called `data/questmod/quests/default.json` and copy the contents of the built-in [`default.json`](src/main/resources/data/questmod/quests/default.json) file
- In the file, find the object at `types/<stat type id>/values`
- Add an entry with key `"<stat value id>"` and value `{ "value": <value> }`
  - Replace `<stat value id>` with the resource location of the quest target, which will usually be an item, block or entity, but may be anything with an identifier
  - To add a base, add `"base": <base>` to the object
  - To mark the quest as a quest that requires death, add `"death": true` to the object
  - To mark the quest as a quest that requires PVP, add `"pvp": true` to the object

Adding quest rewards to the default list:
- Create a file called `data/questmod/quests/default.json` and copy the contents of the built-in [`default.json`](src/main/resources/data/questmod/quests/default.json) file
- In the file, find the list with key `rewards`
- Add an item with the format `{ "id": "<item id>", "value": <value> }`
  - To add a base, add `"base": <base>` to the object, where `<base>` is the 

## Quest Scores
Each target and reward has a score. The longer a world has been played, the higher the score of the quests that will be generated.
The quest score will be added for each target and reward, but the base will only be added once.
The sum of the target scores is equal to the sum of the reward scores.
The reward score includes items and experience points.

The quest score is calculated with the following formula: `total score = base + (value * count)`

## Custom List Definition
Used in [QuestMaker](src/main/java/local/ytk/questmod/quests/QuestMaker.java), [QuestList](src/main/java/local/ytk/questmod/quests/QuestList.java), and [QuestLoader](src/main/java/local/ytk/questmod/quests/QuestLoader.java)

File should be located at `data/<namespace>/quests/<list id>.json`
```json5
{
  "name": "Default Quests", // Name of this quest list (not used)
  "weight": 1000, // How likely this list is to be chosen (irrelevant if there is only one list)
  "types": {
    // Define a target based on a simple criteria
    "<criteria name>": {
      "base": 0, // (optional, defaults to 0) Replace with base
      "value": 10, // Replace with value
      "death": true, // (optional) Include if this is a death quest
      "pvp": true // (optional) Include if this is a PVP quest
    },
    // Define multiple targets with a shared stat type
    "<stat type id>": {
      "type": "<type>",
      "defaultBase": 0, // (optional) Replace with default base
      "default": 10, // Replace with default value
      "death": true, // Include if all of these are death quests
      "pvp": true, // Include if all of these are PVP quests
      // Either "values" or "overrides" must be present, but not both
      // Only includes the values listed here
      "values": {
        // Define a target based on a stat
        "<stat value id>": {
          "base": 10, // (optional) Replace with base
          "value": 10, // (optional) Replace with value
          "death": true, // (optional) Include if this is a death quest
          "pvp": true // (optional) Include if this is a PVP quest
        }
      },
      // Includes all built-in values, but overrides these
      "overrides": {
        "<stat value id>": {
          "base": 10, // (optional) Replace with base
          "value": 10 // (optional) Replace with value
        },
        "<excluded stat value id>": { "exclude": true } // Exclude a value
      }
    },
    // Mining blocks
    "*mined": {
      "noTool": 1,
      "<tool material>": 2,
      "value": 1,
      "hardnessMultiplier": 0.1
    },
    // Crafting items
    "*crafted": {
      "<rarity>Base": 1, // Base for this rarity
      "<rarity>": 2 // Value for this rarity
    }
  },
  "rewards": [
    // Define a reward item
    {
      "id": "<item id>", // Item resource location
      "base": 10, // (optional) Replace with base
      "value": 40 // Replace with value
    },
  ],
  // Configure quest score selection
  "config": {
    "min": 100,
    "min_time_mod": 0.01,
    "min_limit": 640,
    "mid": 500,
    "mid_time_mod": 0.1,
    "mid_limit": 30720,
    "max": 1000,
    "max_time_mod": 0.2,
    "max_limit": 40960
  },
  // Configure target score calculation
  "targetConfig": {
    "prioritize_closer": false,
    "threshold": 0.1
  },
  // Configure reward score calculation and XP calculation
  "rewardConfig": {
    "xp_multiplier": 1.0,
    "max_count": 64,
    "max_shrink": 0.25,
    "max_xp_ratio": 0.5
  }
}
```