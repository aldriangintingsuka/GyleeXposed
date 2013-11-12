package com.gfamily.gyleexposed.Business.Managers;

import java.util.List;

import com.gfamily.gyleexposed.Model.GyleeScript;

import android.content.res.XResources;

public interface IModManager
{
  void ReplaceResource( XResources sourceResources, String packageName, String resourceType, List<GyleeScript> scripts );
}
