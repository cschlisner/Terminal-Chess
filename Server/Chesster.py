#!/usr/bin/python3

# For game communication

import cgi
import rethinkdb as r
import hashlib

print ("Content-type:text/html\r\n\r\n")

form = 			cgi.FieldStorage()
move = 			form["move"].value
game_uuid =		form["game"].value
player_uuid = 	form["uuid"].value

conn = r.connect('localhost', 28015)

def hex_digest(player_uuid):
	m = hashlib.md5()
	m.update(player_uuid.encode('utf-8'))
	return m.hexdigest()

game = r.db("chess").table("games").get(game_uuid).run(conn)

if ((game["white_md5uuid"] != hex_digest(player_uuid)) & (game["black_md5uuid"] != hex_digest(player_uuid))):
	quit()
if ((game["white_md5uuid"] == hex_digest(player_uuid)) & (not game["w"])):
	quit();
if ((game["black_md5uuid"] == hex_digest(player_uuid)) & game["w"]):
	quit();

r.db("chess").table("games").get(game_uuid).update({
  "moves" : r.row["moves"].append(move), 
  "w" : (not game["w"])
}).run(conn)
