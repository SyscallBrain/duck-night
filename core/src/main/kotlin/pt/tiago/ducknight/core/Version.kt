package pt.tiago.ducknight.core

/**
 * Version of the simulation. Any observable change to the simulation or the scoring must bump it
 * and regenerate the affected certificates.
 */
const val SIM_VERSION = 1

/** Version of the simulated photographer models. Certificates record it, and a change regenerates them. */
const val BOT_VERSION = 2

/** Version of the published catalog format written by :leveltool. */
const val CONTENT_VERSION = 1
