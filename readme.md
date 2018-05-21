# friends
A program that fetches lets you fetch and analyze information about your facebook friends network. This program only retrieves information that the other user has made available to you (i.e. private information will not be obtained).

## Usage
[include stuff about how to use this later]

## Compatibility
The program (currently) only runs correctly on Windows 10. Note that there could possibly be a bug with the `java.awt.Robot` class when running on Windows 10 when the display scaling is not at its default value (100% for most desktops and 125% for most laptops).

## Files
- `Person.java`: A class that stores various information about a person, such as `id`, `name`, and `url`. `Person`s are equal when their `id`'s are equal.
- `Graph.java`: A class that represents an undirected graph. Its methods include various graph operations and algorithms.
- `InterruptibleRobot.java`: A class that wraps/extends a `java.awt.Robot` such that it can be interrupted by manually moving the mouse.
- `Harvester.java`: A class that is used to download the dynamically generated `.html` file of a facebook user's Friends page (with all friends loaded on the page), using an `InterruptibleRobot`.
- `FriendsParser.java`: A class that is used to parse the `.html` Friends page that is obtained from `Harvester#harvest()`, extracting information such as a list of the user's friends.
- `Main.java`: The executable class.

## Notes
- More detail about how classes are implemented can be found in the source code.
- There are clearly better ways to implement some of this program's functionalities, but the current implementation is simple to understand (not dependent on any non-standard libraries) and "good enough".

## TODO and other possible improvements
- Revamp file I/O to only use java's files and paths libraries (`java.nio.\*`)