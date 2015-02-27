package com.gfamily.resource.Business.Managers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.gfamily.resource.Model.GyleeScript;

public interface IScriptManager
{
  void LoadScripts() throws IOException;
  void ParseScripts() throws IOException;
  Map<String, List<GyleeScript>> GetScript( String packageName );
}
