package com.gfamily.gyleexposed.Business.Managers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.gfamily.gyleexposed.Model.GyleeScript;

public interface IScriptManager
{
  void LoadScript( String scriptDirectory, String scriptFileName ) throws IOException;
  void ParseScript() throws IOException;
  Map<String, List<GyleeScript>> GetScript( String packageName );
}
