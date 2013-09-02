package com.gfamily.gyleexposed.Model;

public class GyleeScript
{
  private String _resourceName;
  private String _replacementValue;
  private String _description;
  private int _density;

  public GyleeScript( String resourceName, String replacementValue, String description, int density )
  {
    _resourceName = resourceName;
    _replacementValue = replacementValue;
    _description = description;
    _density = density;
  }

  public String GetResourceName()
  {
    return _resourceName;
  }

  public String GetReplacementValue()
  {
    return _replacementValue;
  }

  public String GetDescription()
  {
    return _description;
  }

  public int GetDensity()
  {
    return _density;
  }
}
