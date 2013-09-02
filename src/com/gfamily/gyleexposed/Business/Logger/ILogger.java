package com.gfamily.gyleexposed.Business.Logger;

public interface ILogger
{
  void LogInfo( String message );
  void LogError( Throwable throwable );
}
