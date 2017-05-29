#!/usr/bin/python

"""

"""

from mininet.net import Mininet
from mininet.node import UserSwitch, RemoteController
from mininet.topo import Topo
from mininet.log import lg, info
from mininet.util import irange, quietRun
from mininet.link import TCLink
from mininet.cli import CLI

import sys
flush = sys.stdout.flush

class ExampleTestTopo( Topo ):
    
    def __init__( self, **params ):

        # Initialize topology
        Topo.__init__( self, **params )

        # Create switches and hosts
        N = 6
        hosts = [ self.addHost( 'h%s' % h ) for h in irange( 1, N ) ]
        switches = [ self.addSwitch( 's%s' % s ) for s in irange( 1, N ) ]

        # Wire up switches
        self.addLink( switches[0], switches[1])
        self.addLink( switches[1], switches[2])
        self.addLink( switches[2], switches[3])
        self.addLink( switches[3], switches[4])
        self.addLink( switches[0], switches[5])
        self.addLink( switches[2], switches[5])
        self.addLink( switches[1], switches[4])
        self.addLink( switches[0], switches[3])

      
        # Wire up hosts
        for host, switch in zip( hosts, switches ):
            self.addLink( host, switch )


def bandwidthTest():
   
    # Select TCP Reno
    #output = quietRun( 'sysctl -w net.ipv4.tcp_congestion_control=reno' )
    #assert 'reno' in output
    
    # create network
    print "*** creating network topology"
    topo = ExampleTestTopo()
    sw = UserSwitch
    c0 = RemoteController( 'c0', ip='127.0.0.1', port=6633 )

    net = Mininet( topo=topo, switch=sw, controller=c0, autoSetMacs=True, autoStaticArp=False )
    net.start()
    CLI(net)
    net.stop()




if __name__ == '__main__':
    lg.setLogLevel( 'info' )
    
    print "*** Run traffic demands in example topo"
    bandwidthTest()
