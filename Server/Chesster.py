#!/usr/bin/python3

# For game communication

import cgi
import rethinkdb as r
print ("Content-type:text/html\r\n\r\n")

form = 			cgi.FieldStorage()
move = 			form["move"].value
game_uuid =		form["game"].value
player_uuid = 	form["uuid"].value

conn = r.connect('localhost', 28015)


game = r.db("chess").table("games").get(game_uuid).run(conn)

if ((game["white_uuid"] != player_uuid) & (game["black_uuid"] != player_uuid)):
	quit()
if ((game["white_uuid"] == player_uuid) & (not game["w"])):
	quit();
if ((game["black_uuid"] == player_uuid) & game["w"]):
	quit();

r.db("chess").table("games").get(game_uuid).update({
  "moves" : r.row["moves"].append(move), 
  "w" : (not game["w"])
}).run(conn)


game = r.db("chess").table("games").get(game_uuid).run(conn)


print (game)
