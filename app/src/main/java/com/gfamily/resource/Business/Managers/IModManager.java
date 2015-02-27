package com.gfamily.resource.Business.Managers;

import java.util.List;

import com.gfamily.resource.Model.GyleeScript;

import android.content.res.XResources;

public interface IModManager
{
  void ReplaceResource( XResources sourceResources, String packageName, String resourceType, List<GyleeScript> scripts );
}
