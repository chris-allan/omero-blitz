#!/usr/bin/python

################################################################################
#
# CONFIGURATION
#

uid = None
gid = None
kill = 10

# Environement variables:
#
#   Use DEBUG=1 to turn on Java remote debugging
#
################################################################################

import sys
print """


 OMERO.blitz daemon controller:
 
 This controller starts the OMERO.blitz server along with other processes
 needed by the server. The message below that this is the OMERO.blitz 
 console is NOT true. Ctrl-C will at most close the "tail -f" of the log
 file. Use "kill `cat blitz.pid`" or the name of the pid file as given to
 twistd. This invocation was called with: 

 %(args)s


Note: the following error message may occur several times after start up
while Glacier2 is waiting for OMERO.blitz to startup.

  error: unable to contact permissions verifier `Verifier:tcp -h 127.0.0.1 -p 9999'
  Network.cpp:662: Ice::ConnectionRefusedException:
  connection refused: Connection refused


""" % {"args":sys.argv}

from twisted.internet import protocol, reactor, process
from twisted.runner.procmon import ProcessMonitor, LoggingProtocol
from twisted.application import service, internet

#
# Because of the subshell handling of twisted, it's (currently) possible to
# use a bash login shell, but using a simple "bash -c" causes the wrong paths
# to be set. As a workaround, we're pre-determining the paths now.
# 
import subprocess
proc = subprocess.Popen(['which','java'],stdout=subprocess.PIPE)
java = [proc.communicate()[0].rstrip()]
proc = subprocess.Popen(['which','glacier2router'],stdout=subprocess.PIPE)
rter = [proc.communicate()[0].rstrip()]

import os
if os.environ.has_key("DEBUG"):
	java += ['-Xrunjdwp:server=y,transport=dt_socket,address=9777,suspend=n']

blitz  = java+['-jar','blitz.jar']
router = rter+['--Ice.Config=../etc/glacier2.config']

class Builder(object):

    def buildService(self):
        mon = ProcessMonitor()
        mon.addProcess('OMERO.blitz', blitz,  uid=uid, gid=gid)
        mon.addProcess('Glacier2',    router, uid=gid, gid=gid)
        mon.killTime = kill
        return mon

application = service.Application("blitz")
Builder().buildService().setServiceParent(application)

if __name__ == "__main__":
    import sys
    if len(sys.argv) == 1:
        print "kill -9 `cat twistd.pid`"
    else:
        print "kill -9 `cat "+sys.argv[1]+"/twistd.pid`"
