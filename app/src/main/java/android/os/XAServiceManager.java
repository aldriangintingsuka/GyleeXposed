package android.os;

/**
 * Created by ian on 4/13/16.
 */
public class XAServiceManager
{

  private static XAServiceManager oInstance;

  private IXAService mService;

  public static XAServiceManager GetService()
  {
    if( oInstance == null )
    {
      oInstance = new XAServiceManager();
    }

    return oInstance;
  }

  private XAServiceManager()
  {
    mService = IXAService.Stub.asInterface( ServiceManager.getService( "xa.service" ) );
  }

  public String GetScript( String packageName )
  {
    try
    {
      return mService.GetScript( packageName );

    }
    catch( RemoteException e ) { e.printStackTrace(); }

    return null;
  }
}
