package qgd.middleware.controllers

import play.api.http.LazyHttpErrorHandler

/**
  * The objective is to enable integration of this module in subprojets
  * So assets are defined here.
  *
  * @see https://www.playframework.com/documentation/2.4.x/SBTSubProjects#Assets-and-controller-classes-should-be-all-defined-in-the-controllers.admin-package
  */
class Assets extends controllers.AssetsBuilder(LazyHttpErrorHandler)