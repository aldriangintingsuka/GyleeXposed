package com.gfamily.gyleexposed.Model;

import java.util.Map;

public class GyleeScript
{
  private String _resourceName;
  private Map<String, String> _replacement;
  private String _description;

  public GyleeScript( String resourceName, Map<String, String> replacement, String description )
  {
    _resourceName = resourceName;
    _replacement = replacement;
    _description = description;
  }

  public String GetResourceName()
  {
    return _resourceName;
  }

  public Map<String, String> GetReplacement()
  {
    return _replacement;
  }

  public String GetDescription()
  {
    return _description;
  }
}
