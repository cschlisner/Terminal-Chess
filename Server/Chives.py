#!/usr/bin/python3

# For game creation and user management 

import cgi
import rethinkdb as r
print ("Content-type:text/html\r\n\r\n")



def find_opponent():
	return r.db("chess").table("users").filter({"online":true, "ingame" : false}).nth(0).getField("id").run(conn)

def find_games(player_uuid):
	return r.db("chess").table("games").filter((r.row["white_uuid"] == player_uuid) | (r.row["black_uuid"] == player_uuid)).run(conn)

def create_game(player_one, player_two):
	w = random.choice([True, False])
	return r.db("chess").table('games').insert({
	  "white_uuid" : player_one if w else plyer_two,
	  "black_uuid" : player_two if w else player_one,
	  "w": true,
	  "moves": []
	}).getField("generated_keys").nth(0).run(conn)
	
def register():
	return r.db("chess").table("users").insert({
	  "online" : True, 
	  "ingame" : False
	}).run(conn)["generated_keys"][0]


# switch their online status
def checkin(player):
	in_game = len(find_games(player)) > 0
	r.db("chess").table("users").get(player).update({
		"online" : True, 
		"ingame" : in_game
	}).run(conn)

def checkout(player):
	in_game = len(find_games(player)) > 0
	r.db("chess").table("users").get(player).update({
		"online" : False, 
		"ingame" : in_game
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

elif action == 'retrievegames':
	player = form["uuid"].value
	cursor = find_games(player)
	for doc in cursor:
		print(doc)
