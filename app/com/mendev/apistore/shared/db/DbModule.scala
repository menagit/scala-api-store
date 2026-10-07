package com.mendev.apistore.shared.db

import com.google.inject.{AbstractModule, Provides}
import jakarta.inject.Singleton
import org.apache.pekko.actor.ActorSystem
import play.api.db.Database

class DbModule extends AbstractModule {

  /**
   * Second option to bind in a guice module, need to look up by name
   * to not get the wrong pool (the request pool instead of the jdbc one)
   * @param db
   * @param actorSystem
   * @return
   */
  @Provides
  @Singleton
  def txRunner(db: Database, actorSystem: ActorSystem): TxRunner =
    new PlayDbTxRunner(db, actorSystem.dispatchers.lookup("db-dispatcher"))
}