#!/usr/bin/python3
# For game creation and user management 

import cgi
import rethinkdb as r
import random
print ("Content-type:text/html\r\n\r\n")



def find_opponent():
	return r.db("chess").table("users").filter({
				"online": True, 
				"ingame" : False
			}).nth(0).getField("id").run(conn)

def create_game(player_one, player_two):
	w = random.choice([True, False])
	return r.db("chess").table('games').insert({
	  "white_uuid" : player_one if w else player_two,
	  "black_uuid" : player_two if w else player_one,
	  "w": True,
	  "moves": []
	}).run(conn)["generated_keys"][0]
	
conn = r.connect('localhost', 28015).repl()

while (True):
	online_inlobby = r.db("chess").table("users").filter({
		"online":True, 
		"ingame" : False
	}).run(conn)
	try:
		player_one = online_inlobby.next()
		player_two = online_inlobby.next()	
	except:
		continue;
	
	r.db("chess").table("users").get(player_one["id"]).update({
		"ingame" : True
	}).run(conn)
	r.db("chess").table("users").get(player_two["id"]).update({
		"ingame" : True
	}).run(conn)
	print("created game: " + create_game(player_one["id"], player_two["id"]))