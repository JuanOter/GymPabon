/**
 * In-memory versions of the DAO interfaces, used only by the unit tests. They
 * keep the data in maps instead of MySQL, so the services can be tested fast
 * and without a database (the services only depend on the DAO interfaces).
 */
package com.gympabon.fake;
