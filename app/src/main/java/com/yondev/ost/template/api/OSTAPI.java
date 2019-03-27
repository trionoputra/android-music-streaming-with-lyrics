package com.yondev.ost.template.api;

import com.yondev.ost.template.entity.Audio;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Bakwan on 27/10/2017.
 */

public interface OSTAPI {
    @GET("index.php?r=api/movie")
    Call<List<Audio>> getAll(@Query("id") int id);
}
