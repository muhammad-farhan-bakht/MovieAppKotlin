package com.example.movieapp.repository.HomeRepository
import NetworkBoundResource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.movieapp.DB.AppDatabase
import com.example.movieapp.api.MoviesServices
import com.example.movieapp.model.Movie
import com.example.movieapp.model.MovieResponse
import com.example.movieapp.model.PlayingMovies
import com.example.movieapp.model.UpComingMovies
import com.example.movieapp.utils.ApiResult
import com.example.movieapp.utils.Constant.PLAYING_NOW_CATEGORY
import com.example.movieapp.utils.Constant.POPULAR_CATEGORY
import com.example.movieapp.utils.Constant.TOP_Rated_CATEGORY
import com.example.movieapp.utils.Constant.UP_COMING_CATEGORY
import com.example.movieapp.utils.Resources
import com.example.movieapp.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class HomeRepository constructor(private val localDataSource:AppDatabase, private val remoteDataSource:MoviesServices) {

    fun getPopularMovies(): Flow<Resources<List<Movie>>> {
        return object : NetworkBoundResource<List<Movie>, MovieResponse>(){
            override suspend fun saveNetworkResult(item: MovieResponse) {
                val result = item.results?.map{ movieShow ->
                    Movie(movieShow.backdropPath,movieShow.id,
                        movieShow.originalTitle,movieShow.posterPath,movieShow.title!!,
                        POPULAR_CATEGORY,movieShow.voteAverage)
                }
                localDataSource.movieDao().insertMovies(result!!)
            }
            override suspend fun fetchFromNetwork(): ApiResult<MovieResponse> {
                return safeApiCall(apiCall = {remoteDataSource.getPopularMovies()})
            }

            override fun shouldFetch(data: List<Movie>?): Boolean {
                return  data == null || data.isEmpty()
            }

            override fun loadFromDb(): Flow<List<Movie>> {
                return localDataSource.movieDao().getPopularMovies()
            }

        }.asFlow()
    }
    fun getTopRatedMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config =  PagingConfig(
                pageSize = 10,
                enablePlaceholders = false) ,
            remoteMediator = MoviesRemoteMediator(TOP_Rated_CATEGORY,remoteDataSource,localDataSource),
            pagingSourceFactory = {localDataSource.movieDao().getMovies(TOP_Rated_CATEGORY)}
        ).flow
    }
    fun getUpcomingMovies(): Flow<PagingData<UpComingMovies>> {
        return Pager(
            config =  PagingConfig(
                pageSize = 10,
                enablePlaceholders = false) ,
            remoteMediator = UpcomingRemoteMediotrs(UP_COMING_CATEGORY,remoteDataSource,localDataSource),
            pagingSourceFactory = {localDataSource.upComingMoviesDao().getMovies()}
        ).flow
    }
     fun getPlayNowMovies(): Flow<PagingData<PlayingMovies>> {
        return Pager(
            config =  PagingConfig(
                pageSize = 10,
                enablePlaceholders = false) ,
            remoteMediator = PlayIngNowRemoteMediotrs(PLAYING_NOW_CATEGORY,remoteDataSource,localDataSource),
            pagingSourceFactory = {localDataSource.playingNowMoviesDao().getMovies()}
        ).flow
    }


}