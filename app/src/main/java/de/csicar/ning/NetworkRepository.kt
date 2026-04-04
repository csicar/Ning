package de.csicar.ning

import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val networkDao: NetworkDao) {
    fun getNetwork(networkId: NetworkId): Flow<Network> {
        return networkDao.getById(networkId)
    }
}
