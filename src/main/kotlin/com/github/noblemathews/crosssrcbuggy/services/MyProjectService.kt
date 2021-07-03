package com.github.noblemathews.crosssrcbuggy.services

import com.github.noblemathews.crosssrcbuggy.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
