import web
import random
import time
import sys
from collections import deque

answers = [a.strip() for a in open(sys.argv[2]).readlines() if a.strip() != ""]

print answers

urls = (
  '.*', 'chatbot'
)

history = deque()
for i in range(0, len(answers) / 2):
	history.append("")

class chatbot:
	def GET(self):
		q = web.input(a="",n="",m="",t="")
		time.sleep(1)
		print q.t, q.a, q.n
		print "<", q.m
		
		global history
		m = ""
		while True:
			m = random.choice(answers)
			if not m in history:
				break
		history.append(m)
		history.popleft()
		
		print history
		
		print ">", m
		print
		return m;
		

web.config.debug = False
app = web.application(urls, globals())
app.run()



