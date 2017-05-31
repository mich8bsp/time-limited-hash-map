package com.github.mich8bsp.tlhm;

import akka.actor.ActorSystem;

/**
 * Created by Michael Bespalov on 31-May-17.
 */
public enum MapActorSystem {
    //singleton instance of actor system
    INSTANCE;

    private ActorSystem system = ActorSystem.create("MapActorSystem");

    public static final String MAP_ACTOR_NAME = "MapActor";

    ActorSystem getSystem(){
        return system;
    }
}
