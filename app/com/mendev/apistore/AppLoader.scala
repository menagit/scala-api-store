package com.mendev.apistore

import play.api.{Application, ApplicationLoader, LoggerConfigurator}

class AppLoader extends ApplicationLoader {

  override def load(context: ApplicationLoader.Context): Application = {
    // Guice's loader did this for us; BuiltInComponentsFromContext does not.
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    new AppComponents(context).application
  }
}