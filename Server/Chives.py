#!/usr/bin/python3

# For game creation and user management 

import cgi
import rethinkdb as r
import time
import hashlib
print ("Content-type:text/html\r\n\r\n")

def convert_to_json_because_rethinkdb_sucks(response):
	return response.replace("\'","\"").replace("True","true").replace("False","false")

def find_opponent():
	return r.db("chess").table("users").filter({"online":true, "ingame" : false}).nth(0).getField("id").run(conn)

def hex_digest(player_uuid):
	m = hashlib.md5()
	m.update(player_uuid.encode('utf-8'))
	return m.hexdigest()

def find_games(player_uuid):
	uuidmd5 = hex_digest(player_uuid)
	return r.db("chess").table("games").filter((r.row["white_md5uuid"] == uuidmd5) | (r.row["black_md5uuid"] == uuidmd5)).order_by(r.desc("uts")).run(conn)

def register():
	return r.db("chess").table("users").insert({
	  "online" : True
	}).run(conn)["generated_keys"][0]


# switch their online status
def checkin(player):
	r.db("chess").table("users").get(player).update({
		"online" : True, 
	}).run(conn)

def checkout(player):
	r.db("chess").table("users").get(player).update({
		"online" : False, 
	}).run(conn)

conn = r.connect('localhost', 28015)

form = cgi.FieldStorage()
action = form["action"].value

if action == 'register':
	player = register();
	print(player);
	checkin(player);

elif action == 'checkin':
	player = form["uuid"].value
	checkin(player)

elif action == 'checkout':
	player = form["uuid"].value
	checkout(player)

elif action == 'getgame':
	game_uuid = form["game"].value
	game = r.db("chess").table("games").get(game_uuid).run(conn)
	print(convert_to_json_because_rethinkdb_sucks(str(game)))

elif action == 'retrievegames':
	player = form["uuid"].value
	cursor = find_games(player)
	print(convert_to_json_because_rethinkdb_sucks(str(cursor)))
