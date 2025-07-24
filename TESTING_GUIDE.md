# LifeSteal Plugin Testing Guide

This guide provides a comprehensive approach to testing the LifeSteal plugin, including normal functionality, edge cases, and potential issues to watch for.

## Setup for Testing

### Environment Preparation
- Set up a dedicated test server with minimal plugins to avoid conflicts
- Create 2-3 test accounts for player interaction testing
- Keep a backup of the server before testing to restore if needed
- Make sure the server has sufficient RAM and processing power

### Configuration Preparation
- Make a copy of your default `config.yml` file for reference
- Prepare several test configurations with different settings:
  - Minimal health settings
  - Maximum health settings
  - Disabled features configuration
  - Custom heart item configuration

## Core Functionality Testing

### 1. Health Mechanics

#### Player vs. Player (PvP) Testing
- [ ] Player killing another player gains correct health amount
- [ ] Victim loses correct health amount upon respawn
- [ ] Test when both players are at health limits:
  - Killer at max health
  - Victim at min health
- [ ] Verify bypass permission (`lifesteal.debug.bypass`) prevents health changes

#### Monster/Environment Death Testing
- [ ] Death by various monster types (zombie, skeleton, etc.)
- [ ] Death by environmental causes:
  - Falling damage
  - Drowning
  - Fire/lava damage
  - Suffocation
  - Starvation
- [ ] Verify correct health reduction based on death cause

#### Health Limit Testing
- [ ] Maximum health limit:
  - Players can't exceed configured max health
  - Appropriate message displays when limit reached
- [ ] Minimum health limit:
  - Players can't go below configured min health
  - Test elimination mechanics if implemented (ban/spectator mode)
- [ ] Health persistence across server restarts

### 2. Heart Item Testing

#### Usage Testing
- [ ] Basic heart usage increases health by configured amount
- [ ] Using heart at max health displays appropriate message
- [ ] Using partial hearts (if implemented)
- [ ] Animation and sound effects play correctly
- [ ] Item is consumed properly after use

#### Crafting Testing
- [ ] Heart recipe can be crafted according to configuration
- [ ] Recipe discovery works properly
- [ ] Custom recipes (if multiple types implemented)
- [ ] Recipe displays correctly in recipe book

#### Heart Withdrawal Testing
- [ ] Basic withdrawal creates heart item with correct properties
- [ ] Withdrawal at minimum health limit prevents operation
- [ ] Multiple withdrawals in succession
- [ ] Withdrawal with custom amounts (if implemented)

### 3. Command Testing

#### Health Command
- [ ] `/health` displays current health correctly
- [ ] `/health [player]` shows other player's health (if implemented)
- [ ] Permission checks function correctly
- [ ] Values display in appropriate format (hearts vs. health points)

#### Withdraw Command
- [ ] `/withdraw` creates heart item correctly
- [ ] Amount parameter functions correctly
- [ ] Permission checks function correctly
- [ ] Error messages display when appropriate

#### LifeSteal Admin Commands
- [ ] `/lifesteal reload` reloads configuration
- [ ] `/lifesteal version` displays version info
- [ ] `/lifesteal help` shows available commands
- [ ] Permission checks function correctly

## Edge Case Testing

### 1. Server and Plugin Interactions

#### Server State Testing
- [ ] Plugin functions after server restart
- [ ] Plugin reload functionality works correctly
- [ ] Plugin disables cleanly without errors

#### Game Rule Interactions
- [ ] Test with `keepInventory` set to true/false
- [ ] Test with `ignoreKeepInventory` setting enabled/disabled
- [ ] Test with various difficulty settings

### 2. Multi-Player Scenarios

#### Combat Edge Cases
- [ ] Multiple players damaging victim - who gets the health?
- [ ] Player killing with indirect damage (lava, fall damage, etc.)
- [ ] Player killed by TNT or other player-initiated environmental damage

#### Combat Logging
- [ ] Player disconnecting during combat
- [ ] Health changes apply correctly on reconnect
- [ ] Any anti-combat logging mechanisms function properly

### 3. Technical Edge Cases

#### Permission Testing
- [ ] Test all permission nodes with and without permissions
- [ ] Test with permission plugin hierarchy

#### Performance Testing
- [ ] Server performance with many players (10+)
- [ ] Multiple simultaneous deaths
- [ ] Recipe discovery task performance
- [ ] Heart item usage in rapid succession

#### Error Handling
- [ ] Corrupted configuration file recovery
- [ ] Invalid configuration values
- [ ] Missing dependencies
- [ ] Plugin conflicts resolution

## World-Specific Testing

### Multi-World Testing
- [ ] Deaths in different worlds
- [ ] World-specific settings (if implemented)
- [ ] Cross-world teleportation with different health values
- [ ] Nether/End world interactions

## Compatibility Testing

### Plugin Compatibility
- [ ] Test with other health-modifying plugins
- [ ] Test with PvP/combat management plugins
- [ ] Permission plugin integration
- [ ] Economy plugin integration (if relevant)

### Minecraft Version Compatibility
- [ ] Test on all supported Minecraft versions
- [ ] Check for API changes between versions
- [ ] Verify heart item appearance across versions

## User Experience Testing

### UI and Feedback
- [ ] All messages are clear and informative
- [ ] Color formatting is consistent and readable
- [ ] Sound effects provide appropriate feedback
- [ ] Visual effects are noticeable but not distracting

### Command Usability
- [ ] Tab completion works for all commands
- [ ] Command syntax is intuitive
- [ ] Error messages clearly explain issues
- [ ] Help information is comprehensive

## Detailed Test Cases

### Test Case 1: Basic PvP Health Transfer
1. Setup: Two players with default health (Player A and Player B)
2. Action: Player A kills Player B
3. Expected Result: 
   - Player A gains `playerKillHealthGained` amount of health
   - Player B loses `playerDeathHealthLost` amount of health on respawn
4. Verification: Check both players' health values using `/health` command

### Test Case 2: Heart Item at Max Health
1. Setup: Set player to 1 heart below max health limit
2. Action: Player uses heart item worth 2 hearts
3. Expected Result: Player should receive error message about exceeding max health
4. Verification: Player's health remains unchanged, item not consumed

### Test Case 3: Withdrawal Permission Test
1. Setup: Two players - one with withdraw permission, one without
2. Action: Both attempt to use `/withdraw` command
3. Expected Result: Only player with permission succeeds
4. Verification: Check command output and inventory contents

### Test Case 4: Death by Monster at Minimum Health
1. Setup: Set player to minimum health limit + monster kill health loss amount
2. Action: Player dies to a zombie
3. Expected Result: Player's health reduces to minimum limit but not below
4. Verification: Check player's health after respawn

### Test Case 5: Config Reload Test
1. Setup: Running server with active players
2. Action: Modify config file and execute reload command
3. Expected Result: New settings apply without server restart
4. Verification: Test affected functionality with new settings

## Regression Testing

After making any changes to the plugin:

1. Run through all core functionality tests
2. Check previously identified and fixed bugs
3. Verify compatibility with supported Minecraft versions
4. Test performance impact of changes

## Issue Tracking Template

When documenting issues found during testing:

```
Issue ID: [Unique ID]
Summary: [Brief description]
Steps to Reproduce:
1. [Step 1]
2. [Step 2]
3. [Step 3]

Expected Result: [What should happen]
Actual Result: [What actually happened]
Environment: [Server version, Plugin version, Relevant config settings]
Screenshots/Logs: [If applicable]
Severity: [Critical/Major/Minor/Cosmetic]
```

## Final Verification Checklist

Before releasing:

- [ ] All core functionality tests pass
- [ ] All known issues are resolved or documented
- [ ] Performance is acceptable under load
- [ ] Documentation is complete and accurate
- [ ] Version information is updated
- [ ] Backup and rollback procedures are tested
