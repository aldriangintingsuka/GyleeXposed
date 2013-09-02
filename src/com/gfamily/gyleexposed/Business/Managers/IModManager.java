package com.gfamily.gyleexposed.Business.Managers;

import java.util.List;

import com.gfamily.gyleexposed.Model.GyleeScript;

import android.content.res.XModuleResources;
import android.content.res.XResources;

public interface IModManager
{
  void ReplaceResource( XModuleResources moduleResources, XResources sourceResources, String packageName, String resourceType, List<GyleeScript> scripts );
  void ForwardDrawable( XModuleResources moduleResources, XResources sourceResources, String packageName, String resourceType, String sourceName, String targetName );
}
