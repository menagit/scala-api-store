package shared.db

import com.google.inject.{AbstractModule, Provides}
import jakarta.inject.Singleton
import org.apache.pekko.actor.ActorSystem
import play.api.db.Database

class DbModule extends AbstractModule {

  @Provides
  @Singleton
  def txRunner(db: Database, actorSystem: ActorSystem): TxRunner =
    new PlayDbTxRunner(db, actorSystem.dispatchers.lookup("db-dispatcher"))
}