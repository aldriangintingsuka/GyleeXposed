package com.gfamily.resource.Model;

import java.util.List;
import java.util.Map;

public class GyleeScript
{
  private String _resourceName;
  private List<Map<String, String>> _replacements;
  private String _description;

  public GyleeScript( String resourceName, List<Map<String, String>> replacements, String description )
  {
    _resourceName = resourceName;
    _replacements = replacements;
    _description = description;
  }

  public String GetResourceName()
  {
    return _resourceName;
  }

  public List<Map<String, String>> GetReplacements()
  {
    return _replacements;
  }

  public String GetDescription()
  {
    return _description;
  }

}
