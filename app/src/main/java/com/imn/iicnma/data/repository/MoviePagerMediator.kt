package com.imn.iicnma.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.bumptech.glide.load.HttpException
import com.imn.iicnma.data.local.MovieDatabase
import com.imn.iicnma.data.local.keys.RemoteKeysEntity
import com.imn.iicnma.data.local.movie.MovieEntity
import com.imn.iicnma.data.remote.MovieService
import com.imn.iicnma.data.remote.STARTING_PAGE_INDEX
import java.io.IOException
import java.io.InvalidObjectException

@OptIn(ExperimentalPagingApi::class)
class MoviePagerMediator(
    private val service: MovieService,
    private val movieDatabase: MovieDatabase
) : RemoteMediator<Int, MovieEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>
    ): MediatorResult {
        val pageKey = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKey = getRemoteKeyClosestToCurrentPosition(state)
                remoteKey?.nextKey?.minus(1) ?: STARTING_PAGE_INDEX
            }
            LoadType.PREPEND -> {
                val remoteKey = getRemoteKeyForFirstItem(state)
                    ?: throw InvalidObjectException("Remote key and the prevKey should not be null")
                remoteKey.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                val remoteKey = getRemoteKeyForLastItem(state)
                if (remoteKey?.nextKey == null) {
                    throw InvalidObjectException("Remote key should not be null for $loadType")
                }
                remoteKey.nextKey
            }
        }

        try {
            val apiResponse = service.getPopularMovies(pageKey)

            movieDatabase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    movieDatabase.remoteKeysDao().clearRemoteKeys()
                    movieDatabase.moviesDao().clearMovies()
                }
                val prevKey = if (pageKey == STARTING_PAGE_INDEX) null else pageKey - 1
                val nextKey = if (pageKey >= apiResponse.totalPages) null else pageKey + 1
                val keys = apiResponse.results.map { RemoteKeysEntity(it.id, prevKey, nextKey) }
                movieDatabase.remoteKeysDao().insertAll(keys)
                movieDatabase.moviesDao().insertAll(apiResponse.toMovieEntityList())
            }
            return MediatorResult.Success(endOfPaginationReached = pageKey >= apiResponse.totalPages)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, MovieEntity>): RemoteKeysEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { movie -> movieDatabase.remoteKeysDao().remoteKeysMovieId(movie.id) }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, MovieEntity>): RemoteKeysEntity? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { movie -> movieDatabase.remoteKeysDao().remoteKeysMovieId(movie.id) }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, MovieEntity>): RemoteKeysEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)
                ?.id?.let { movieId -> movieDatabase.remoteKeysDao().remoteKeysMovieId(movieId) }
        }
    }
}