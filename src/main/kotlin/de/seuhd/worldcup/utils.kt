package de.seuhd.worldcup

/**
 * Check if the grou p is empty
 * Wrote this, coz this was repeated across functions
 */
fun checkifGroupisEmpty(allGroups: List<Group>): List<Group>? {
    if (allGroups.isEmpty()) {
        println("No groups available.")
        return null
    }
    return allGroups
}

fun displayHeader(name: String){
    println("\n=================================================")
    println("                  ${name}")
    println("=================================================")
}

fun printClosingLine(){
    println("=================================================\n")
}

/**
 * Looks for group, if group exists, returns group, else
 * throws error, returns null
 * Wrote this, coz this was repeated across functions
 */
fun getGroupFromInput(allGroups: List<Group>,question:String, name: String): Group? {
    displayHeader(name)
    print(question)
    val allGroupsChecked = checkifGroupisEmpty(allGroups) ?: return null
    val inputGroup = readln().trim().lowercase()
    val group = allGroupsChecked.find { it.name.lowercase() == inputGroup }

    if (group == null) {
        println("Group not found")
        return null
    }
    if (group.matches.isEmpty()) {
        println("No matches found for ${group.name}")
        return null
    }
    return group
}