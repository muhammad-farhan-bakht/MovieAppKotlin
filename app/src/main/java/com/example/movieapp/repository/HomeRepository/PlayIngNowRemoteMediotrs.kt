package com.example.movieapp.repository.HomeRepository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.movieapp.DB.AppDatabase
import com.example.movieapp.api.MoviesServices
import com.example.movieapp.model.Movie
import com.example.movieapp.model.MoviesRemoteKeys
import com.example.movieapp.model.PlayingMovies
import com.example.movieapp.model.UpComingMovies

import com.example.movieapp.utils.Constant.PLAYING_NOW_CATEGORY
import com.example.movieapp.utils.Constant.TOP_Rated_CATEGORY
import com.example.movieapp.utils.Constant.UP_COMING_CATEGORY
import retrofit2.HttpException
import java.io.IOException
import java.security.Key
import java.util.ArrayList

@OptIn(ExperimentalPagingApi::class)
class PlayIngNowRemoteMediotrs(
    private val category: String,
    private val moviesService: MoviesServices,
    private val moviesDatabase: AppDatabase
) : RemoteMediator<Int, PlayingMovies>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PlayingMovies>
    ): MediatorResult {
        return try {
            // 1
            val loadKeys= when (loadType) {
                LoadType.REFRESH ->{
                    null}
                LoadType.PREPEND -> {

                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    getMovieRemoteKeys()
                }
            }

            // 2

            var key =loadKeys?.afterIndex?:1
            if (loadType == LoadType.REFRESH ){
                val newkey = getMovieRemoteKeys()?.afterIndex?.minus(1)
                key = newkey?:key
            }
            val response = fetchCategory(key)
            val data = response!!
            moviesDatabase.withTransaction {
               moviesDatabase.movieRemoteKeys()
                    .insertKeys(MoviesRemoteKeys(0,key+1,key, category))
                moviesDatabase.playingNowMoviesDao().insertMovies(data)
            }
            val isEmpty = data .isEmpty()
            MediatorResult.Success(endOfPaginationReached = !isEmpty)
        } catch (exception: IOException) {
            MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            MediatorResult.Error(exception)
        }

    }

    private suspend fun getMovieRemoteKeys(): MoviesRemoteKeys? {
        return moviesDatabase.movieRemoteKeys().getMoviesKeys(category).firstOrNull()
    }
    private suspend fun fetchCategory(pageNumber:Int):List<PlayingMovies>?{
        val response = moviesService.getPlayingNowMovies(page = pageNumber)
        println(response.toString())
        val list = ArrayList<PlayingMovies>()
        response.results?.forEach {
            list.add(
               PlayingMovies(it.backdropPath,it.id,
                it.originalTitle,it.posterPath,it.title!!,category, it.voteAverage
                )
            )
        }
        return list
    }

}