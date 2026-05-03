package de.seuhd.worldcup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

val userBets = mutableListOf<Bet>()

fun main() {
    //TODO: Load JSON data
    println("Enter your input: ")
    var data = WorldCupData(
        tournament = "FIFA World Cup 2026",
        groups = emptyList(),
        knockouts = emptyList()
    )
    val filePath = "src/main/resources/world_cup_2026_full_data.json"
    try {
        val jsonString = File(filePath).readText()

        data = Json.decodeFromString<WorldCupData>(jsonString)

        println("Worldcup data: $data")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
    //TODO: Implement interactive menu
    showMenu(data)
}

fun showMenu(worldCupData: WorldCupData) {
    var isOpen  = true
    while (isOpen) {
        println("\n ===== FIFA World Cup 2026 ? Betting Console =====")
        println("1) Show Standings")
        println("2) Show Matches")
        println("3) Place Bets")
        println("4) Show Betting Score")
        println("5) Exit")
        printClosingLine()
        print("Choose an option (1 to 5): ")

        val choice = readln().trim()

        when (choice) {
            "1" -> showStandings(worldCupData.groups)
            "2" -> showMatches(worldCupData.groups)
            "3" -> placeBets(worldCupData.groups)
            "4" -> showBettingScore(worldCupData.groups)
            "5" -> {
                println("\nExiting, bye")
                isOpen = false // Stops the loop
            }

            else -> println("Invalid choice, try again.")
        }
    }
}
///* -------------------------------------------------------------
//   1) Show Standings
//   ------------------------------------------------------------- */
private fun showStandings(allGroups: List<Group>) {
    var allGroups = checkifGroupisEmpty(allGroups) ?: return

    println("\nWhich view do you want to see?")
    println("1) View a single group")
    println("2) View all groups")
    print("Choose an option (1 or 2): ")

    // read the user input
    val choice = readln().trim()

    // switch case
    when (choice) {
        // Single group view
        "1" -> {
            print("\nEnter group name to view: ")
            val inputName = readln().trim().lowercase()

            val targetGroup = allGroups.find { it.name.lowercase() == inputName }
            if (targetGroup != null) {
                printGroupStandings(targetGroup)
            } else {
                println("Group not found")
            }
        }
        // All group view
        "2" -> {
            allGroups.forEach { printGroupStandings(it) }
        }
        else -> println("Invalid option, try again.")
    }
}

private fun printGroupStandings(group: Group) {
    class TeamStats(
        val name: String,
        var points: Int = 0,
        var goalDifference: Int = 0
    )
    val statsMap = group.teams.associate { it.name to TeamStats(it.name) }.toMutableMap()
    group.matches.forEach { match ->
        val homeStats = statsMap[match.homeTeam]
        val awayStats = statsMap[match.awayTeam]
        val homeScore = match.homeScore ?: 0
        val awayScore = match.awayScore ?: 0

        // Update Goal Difference
        homeStats?.goalDifference = homeStats?.goalDifference?.plus(homeScore - awayScore) ?: 0
        awayStats?.goalDifference = awayStats?.goalDifference?.plus(awayScore - homeScore) ?: 0

        if (homeScore > awayScore) {
            homeStats?.points = homeStats?.points?.plus(3) ?: 0
        } else if (homeScore < awayScore) {
            awayStats?.points = awayStats?.points?.plus(3) ?: 0
        } else {
            homeStats?.points = homeStats?.points?.plus(1) ?: 0
            awayStats?.points = awayStats?.points?.plus(1) ?: 0
        }
    }
    val sortedStats = statsMap.values.sortedWith(
        compareByDescending<TeamStats> { it.points }
            .thenByDescending { it.goalDifference }
    )
    displayHeader(group.name)
    sortedStats.forEachIndexed { index, stats ->
        println("  ${index + 1}. ${stats.name.padEnd(8)} | Points: ${stats.points.toString().padEnd(2)} | Goal Diff: ${stats.goalDifference}")
    }
    printClosingLine()
}
//
///* -------------------------------------------------------------
//   2) Show Matches
//   ------------------------------------------------------------- */
private fun showMatches(allGroups: List<Group>) {
    val group = getGroupFromInput(allGroups,"Which group's matches do you want to view?", "SHOW MATCH SCHEDULE") ?: return

    println("\n Matches for ${group.name}:")

    group.matches.forEachIndexed { index, match ->
        val scoreDisplay = if (match.homeScore != null && match.awayScore != null) {
            "${match.homeScore} - ${match.awayScore}"
        } else {
            "Not played yet"
        }
        println("   ${index + 1}. [${match.round}] ${match.date} | ${match.homeTeam} vs ${match.awayTeam} at ${match.ground} | Score: $scoreDisplay")
    }

    printClosingLine()
}
//
///* -------------------------------------------------------------
//   3) Place Bets
//   ------------------------------------------------------------- */
private fun placeBets(allGroups: List<Group>) {
    val group = getGroupFromInput(allGroups, "Which group's matches do you want to bet on?","PLACE YOUR BET") ?: return
    val matchesToBetOn = group.matches
    println("\nPlacing bets for ${group.name}:")
    for (match in matchesToBetOn) {
        var validTip: Int? = null

        while (validTip == null) {
            print("Match: ${match.homeTeam} vs ${match.awayTeam} (Date: ${match.date}) | Enter tip [1 = Home, 2 = Away, 0 = Draw]: ")
            val input = readln().trim().toIntOrNull()

            if (input != null && input in listOf(0, 1, 2)) {
                validTip = input

                // Map tip number to a readable label
                val tipLabel = when (validTip) {
                    1 -> "Home Win"
                    2 -> "Away Win"
                    else -> "Draw"
                }

                // Add or update the bet in the collection
                userBets.removeAll { it.matchId == match.matchId }
                userBets.add(
                    Bet(
                        matchId = match.matchId,
                        homeTeam = match.homeTeam,
                        awayTeam = match.awayTeam,
                        tip = validTip
                    )
                )

                println("Bet placed: $tipLabel")
            } else {
                println("Invalid input. Please enter 1, 2, or 0.")
            }
        }
    }
    printClosingLine()
}
//
///* -------------------------------------------------------------
//   4) Show Betting Score
//   ------------------------------------------------------------- */
private fun showBettingScore(allGroups: List<Group>) {
    displayHeader("YOUR BETTING SCORE")
    if (userBets.isEmpty()) {
        println("You haven't placed any bets yet.")
        printClosingLine()
        return
    }
    var totalScore = 0
    var correctPredictions = 0
    var incorrectPredictions = 0

    println("\n--- Prediction Summary ---")

    for (bet in userBets) {
        val actualMatch = allGroups.flatMap { it.matches }.find { it.matchId == bet.matchId }

        if (actualMatch != null) {
            // Check if the match has concluded
            val homeScore = actualMatch.homeScore
            val awayScore = actualMatch.awayScore

            if (homeScore != null && awayScore != null) {
                val actualOutcome = when {
                    homeScore > awayScore -> 1 // Home Win
                    homeScore < awayScore -> 2 // Away Win
                    else -> 0                  // Draw
                }

                // Check if the user's bet matches the result
                if (bet.tip == actualOutcome) {
                    totalScore += 1
                    correctPredictions += 1
                    println("Match ${bet.matchId} (${bet.homeTeam} vs ${bet.awayTeam}): CORRECT")
                } else {
                    incorrectPredictions += 1
                    println("Match ${bet.matchId} (${bet.homeTeam} vs ${bet.awayTeam}): INCORRECT")
                }
            } else {
                println("Match ${bet.matchId} (${bet.homeTeam} vs ${bet.awayTeam}): Match not played yet.")
            }
        } else {
            println("Match ${bet.matchId}: Match not found in the data.")
        }
    }
    printClosingLine()
    println("TOTAL SCORE: $totalScore Points")
    println("Correct Predictions: $correctPredictions")
    println("Incorrect Predictions: $incorrectPredictions")
    printClosingLine()
}